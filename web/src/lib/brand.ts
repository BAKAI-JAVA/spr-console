/**
 * The console's brand: what it calls the product, its mark, its favicon, and every name built
 * from the slug (cookies).
 */
import favicon from '#lib/assets/favicon.svg';

export const BRAND = {
	/** What a person reads. */
	name: 'Стоп-факторы',

	/** What the cookie names are built from: lower-case letters and digits. */
	slug: 'stopfactor',

	/** The mark drawn beside the name: a Remix Icon class. */
	icon: 'ri-shield-check-line',

	/** Imported, so Vite builds it into the app and the URL works in production. */
	favicon
} as const;

/** A page's <title>: its parts, most specific first, the empty ones left out, then the product. */
export function pageTitle(...parts: (string | null | undefined)[]): string {
	return [...parts, BRAND.name].filter(Boolean).join(' · ');
}

export const COOKIES = {
	theme: `${BRAND.slug}-theme`,

	sidebar: `${BRAND.slug}-sidebar`,

	branches: `${BRAND.slug}-sidebar-closed`,

	/** Who makes the changes: this server sends it to the API as X-User, the author of a version. */
	user: `${BRAND.slug}-user`
} as const;
