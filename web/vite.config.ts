import { defineConfig } from 'vitest/config';
import adapter from '@sveltejs/adapter-node';
import { sveltekit } from '@sveltejs/kit/vite';

export default defineConfig({
	// The dev server renders the pages, and src/hooks.server.ts passes the API's paths on to the
	// stop factor backend (API_URL, src/env.ts), as the built server does in production.
	server: {
		port: 5173,
		strictPort: true
	},

	plugins: [
		sveltekit({
			compilerOptions: {
				// Force runes mode for the project, except for libraries. Can be removed in svelte 6.
				runes: ({ filename }) =>
					filename.split(/[/\\]/).includes('node_modules') ? undefined : true
			},

			// A Node server (`node build`) that renders each page before sending it and passes the
			// API's paths on (hooks.server.ts).
			adapter: adapter(),

			// Where browsers reach the app, given when it is built (ORIGIN): without it adapter-node
			// guesses from the request's headers and takes https when none say.
			paths: { origin: process.env.ORIGIN || undefined },

			// Every script is the app's own: the ones SvelteKit writes into a page carry a fresh
			// nonce, the rest are files from 'self', so an injected <script> does not run. Styles
			// stay inline-able (Svelte's transitions and component style attributes need it).
			csp: {
				mode: 'auto',
				directives: {
					'default-src': ['self'],
					'script-src': ['self'],
					'style-src': ['self', 'unsafe-inline'],
					'img-src': ['self', 'data:'],
					'font-src': ['self'],
					'connect-src': ['self'],
					'object-src': ['none'],
					'base-uri': ['none'],
					'form-action': ['self'],
					'frame-ancestors': ['none']
				}
			}
		})
	],

	test: {
		expect: { requireAssertions: true },
		environment: 'node',
		include: ['src/**/*.{test,spec}.{js,ts}']
	}
});
