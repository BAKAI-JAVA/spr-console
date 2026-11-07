import { describe, expect, it } from 'vitest';
import type { Metric, Rule } from '#lib/api/index.js';
import { paramsText, rulesSummary } from './rules';

const rule = (active?: boolean): Rule => ({ params: {}, conditions: [], active });

describe('rulesSummary', () => {
	const tests: { name: string; rules: Rule[]; want: string }[] = [
		{ name: 'one rule', rules: [rule()], want: '1 правило' },
		{ name: 'a few rules', rules: [rule(), rule(true), rule()], want: '3 правила' },
		{
			name: 'some turned off',
			rules: [rule(), rule(false), rule(), rule(), rule()],
			want: '4 из 5 правил'
		}
	];

	for (const { name, rules, want } of tests) {
		it(name, () => expect(rulesSummary({ rules })).toBe(want));
	}
});

describe('paramsText', () => {
	const metric = {
		params: [
			{ key: 'source', label: 'Источник', required: true },
			{ key: 'periodFromMonth', label: 'С месяца', required: true }
		]
	} as Metric;

	it("names the params in the metric's order", () => {
		expect(paramsText({ ...rule(), params: { periodFromMonth: 1, source: 'KIB' } }, metric)).toBe(
			'Источник: KIB · С месяца: 1'
		);
	});

	it('says when there are none', () => {
		expect(paramsText(rule(), metric)).toBe('Без параметров');
	});
});
