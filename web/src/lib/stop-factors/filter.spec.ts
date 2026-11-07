import { describe, expect, it } from 'vitest';
import type { StopFactor } from '#lib/api/index.js';
import { isFiltered, matchesSearch, parseFilter } from './filter';

describe('parseFilter', () => {
	const tests: { name: string; query: string; want: ReturnType<typeof parseFilter> }[] = [
		{
			name: 'every filter',
			query: 'status=inactive&segment=MSB&metricId=10&productId=5',
			want: { active: false, segment: 'MSB', metricId: 10, productId: 5 }
		},
		{ name: 'active ones', query: 'status=active', want: { active: true } },
		{
			name: 'values a link may have wrong',
			query: 'status=all&segment=msb&metricId=ten&productId=-1',
			want: {}
		}
	];

	for (const { name, query, want } of tests) {
		it(name, () => {
			const filter = parseFilter(new URLSearchParams(query));
			const defined = Object.fromEntries(Object.entries(filter).filter(([, v]) => v !== undefined));
			expect(defined).toEqual(want);
		});
	}

	it('is not a filter when nothing is set', () => {
		expect(isFiltered(parseFilter(new URLSearchParams('')))).toBe(false);
	});
});

describe('matchesSearch', () => {
	const sf = {
		factorId: 13,
		name: 'Количество непрерывной просроченной задолженности',
		metricTitle: 'Просрочки',
		description: null
	} as StopFactor;

	const tests: { name: string; search: string; want: boolean }[] = [
		{ name: 'nothing typed', search: ' ', want: true },
		{ name: 'its number', search: '13', want: true },
		{
			name: 'words of its name in any case and order',
			search: 'ПРОСРОЧЕННОЙ непрерывной',
			want: true
		},
		{ name: 'its metric', search: 'просрочки', want: true },
		{ name: 'a word it does not have', search: 'реструктуризаций', want: false }
	];

	for (const { name, search, want } of tests) {
		it(name, () => expect(matchesSearch(sf, search)).toBe(want));
	}
});
