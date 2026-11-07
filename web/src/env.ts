import { defineEnvVars } from '@sveltejs/kit/env';

// API_URL is where the stop factor backend (core-console) listens, which the browser never sees:
// this server passes the API's paths on to it. Development runs it on this machine.
export const variables = defineEnvVars({
	API_URL: { schema: (input) => input || 'http://localhost:9090' }
});
