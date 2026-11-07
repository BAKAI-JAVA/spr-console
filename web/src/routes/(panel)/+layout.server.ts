import { COOKIES } from '#lib/brand.js';
import { readChoice } from '#lib/utils/cookies.js';
import { isSidebarState, parseClosedGroups, type SidebarState } from '#lib/state/sidebar.js';
import type { LayoutServerLoad } from './$types';

export const load: LayoutServerLoad = async ({ cookies, url }) => {
	const saved = readChoice(cookies, COOKIES.sidebar, url);
	const sidebar: SidebarState = isSidebarState(saved) ? saved : 'wide';
	const closedGroups = parseClosedGroups(readChoice(cookies, COOKIES.branches, url));
	const user = readChoice(cookies, COOKIES.user, url) ?? '';

	return { sidebar, closedGroups, user };
};
