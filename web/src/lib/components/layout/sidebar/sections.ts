import type { RouteId } from '$app/types';
import type { IconName } from '#lib/components/ui/index.js';

export type Section = Extract<Exclude<RouteId, `${string}[${string}`>, `/(panel)${string}`>;

export type SidebarItem = {
	route: Section;
	label: string;
	icon: IconName;
};

export type SidebarGroup = {
	/** A name the cookie can hold, so which groups are folded survives a
	    reload. It is not the label: renaming a group should not unfold it. */
	id: string;
	label: string;
	items: SidebarItem[];
};

export const groups: SidebarGroup[] = [
	{
		id: 'decisions',
		label: 'Решения',
		items: [
			{ route: '/(panel)/stop-factors', label: 'Стоп-факторы', icon: 'ri-shield-check-line' },
			{ route: '/(panel)/metrics', label: 'Метрики', icon: 'ri-function-line' }
		]
	},
	{
		id: 'catalog',
		label: 'Справочники',
		items: [{ route: '/(panel)/products', label: 'Продукты', icon: 'ri-stack-line' }]
	}
];

/** The current page's icon is drawn filled: the `-fill` twin of its `-line` icon. */
export function currentIcon(icon: IconName): IconName {
	return icon.replace(/-line$/, '-fill');
}

/**
 * Whether `route` is the page being read, compared by route id (resolved paths are relative
 * and never match the pathname); a section also matches the pages below it.
 */
export function isCurrentSection(route: Section, id: string | null | undefined): boolean {
	return route === '/(panel)' ? id === route : id === route || Boolean(id?.startsWith(`${route}/`));
}
