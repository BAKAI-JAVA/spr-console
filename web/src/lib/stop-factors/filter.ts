import type { ProductSegment, StopFactor, StopFactorFilter } from '#lib/api/index.js';

const SEGMENTS: readonly string[] = ['RETAIL', 'MSB'] satisfies ProductSegment[];

const id = (value: string | null) => (value && /^\d+$/.test(value) ? Number(value) : undefined);

/**
 * The list's filter from its address: ?status=active|inactive&segment=MSB&metricId=10&productId=5.
 * Anything else is no filter, so an edited or stale link still opens the list.
 */
export function parseFilter(params: URLSearchParams): StopFactorFilter {
	const status = params.get('status');
	const segment = params.get('segment') ?? '';

	return {
		active: status === 'active' ? true : status === 'inactive' ? false : undefined,
		metricId: id(params.get('metricId')),
		productId: id(params.get('productId')),
		segment: SEGMENTS.includes(segment) ? (segment as ProductSegment) : undefined
	};
}

export const isFiltered = (filter: StopFactorFilter): boolean =>
	Object.values(filter).some((value) => value !== undefined);

/** Whether a stop factor matches what was typed in the search: its №, name, metric or description. */
export function matchesSearch(stopFactor: StopFactor, search: string): boolean {
	const words = search.trim().toLowerCase();
	if (!words) return true;

	const text = [
		stopFactor.factorId,
		stopFactor.name,
		stopFactor.metricTitle,
		stopFactor.description
	]
		.filter(Boolean)
		.join(' ')
		.toLowerCase();

	return words.split(/\s+/).every((word) => text.includes(word));
}
