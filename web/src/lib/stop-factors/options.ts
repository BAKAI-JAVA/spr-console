import type { SelectOption } from '#lib/components/ui/index.js';
import { SEGMENTS } from './labels';

/** Ark's select reads an empty value as nothing chosen, so "all" has a value of its own. */
export const ALL = 'all';

/** Options led by an "all" choice. */
export function withAll(label: string, options: SelectOption[]): SelectOption[] {
	return [{ value: ALL, label }, ...options];
}

/** A select's value as a filter: undefined for "all". */
export const unlessAll = (value: string): string | undefined => (value === ALL ? undefined : value);

export const segmentOptions = (allLabel = 'Все сегменты'): SelectOption[] =>
	withAll(
		allLabel,
		Object.entries(SEGMENTS).map(([value, label]) => ({ value, label }))
	);
