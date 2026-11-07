import { describe, expect, it } from 'vitest';
import { countOf, initials } from './format';

describe('countOf', () => {
	const tests: { n: number; want: string }[] = [
		{ n: 1, want: '1 правило' },
		{ n: 2, want: '2 правила' },
		{ n: 5, want: '5 правил' },
		{ n: 11, want: '11 правил' },
		{ n: 21, want: '21 правило' },
		{ n: 112, want: '112 правил' },
		{ n: 1000, want: '1 000 правил' }
	];

	for (const { n, want } of tests) {
		it(`${n}`, () =>
			expect(countOf(n, 'правило', 'правила', 'правил').replace(/\s/g, ' ')).toBe(want));
	}
});

describe('initials', () => {
	it('takes the first letters of two words, or two letters of one', () => {
		expect(initials('Алибек Алтынбеков')).toBe('АА');
		expect(initials('a.altynbek')).toBe('AA');
		expect(initials('admin')).toBe('AD');
	});
});
