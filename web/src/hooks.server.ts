import type { Handle } from '@sveltejs/kit/hooks';

import { COOKIES } from '#lib/brand.js';
import { isApiPath, toApi } from '#lib/server/api.js';
import { setPageHeaders } from '#lib/server/headers.js';
import { isTheme } from '#lib/theme.js';
import { readChoice } from '#lib/utils/cookies.js';

export const handle: Handle = async ({ event, resolve }) => {
	if (isApiPath(event.url.pathname)) {
		return toApi(event, readChoice(event.cookies, COOKIES.user, event.url));
	}

	// The reader's theme is written into <html> as the page is rendered, so
	// a dark page never flashes light and no script has to guess it.
	const theme = readChoice(event.cookies, COOKIES.theme, event.url);
	const response = await resolve(event, {
		transformPageChunk: ({ html }) => html.replace('%theme%', isTheme(theme) ? theme : '')
	});
	setPageHeaders(response.headers, event.url);

	return response;
};
