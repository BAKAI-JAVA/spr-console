import { describe, expect, it } from 'vitest';
import type { Condition } from '#lib/api/index.js';
import { conditionText, hasValueTo, sortConditions } from './conditions';

describe('conditionText', () => {
	const tests: {
		name: string;
		condition: Pick<Condition, 'operator' | 'value' | 'valueTo'>;
		want: string;
	}[] = [
		{ name: 'a threshold', condition: { operator: 'GT', value: '8' }, want: 'x > 8' },
		{ name: 'a lower bound', condition: { operator: 'GTE', value: '2' }, want: 'x ≥ 2' },
		{ name: 'an exact value', condition: { operator: 'EQ', value: '0' }, want: 'x = 0' },
		{
			name: 'the yellow zone up to a rejection',
			condition: { operator: 'RANGE', value: '5', valueTo: '8' },
			want: '5 < x ≤ 8'
		},
		{
			name: 'a range with both ends',
			condition: { operator: 'BETWEEN', value: '6', valueTo: '8' },
			want: '6 ≤ x ≤ 8'
		},
		{
			name: 'loan statuses',
			condition: { operator: 'IN', value: 'Closed, Sold' },
			want: 'статус ∈ Closed, Sold'
		},
		{ name: 'a legacy condition without an operator', condition: { value: '3' }, want: '3' }
	];

	for (const { name, condition, want } of tests) {
		it(name, () => expect(conditionText(condition)).toBe(want));
	}
});

describe('hasValueTo', () => {
	it('is true for the operators with an upper bound only', () => {
		expect(['RANGE', 'BETWEEN'].every((op) => hasValueTo(op as Condition['operator']))).toBe(true);
		expect(
			['GT', 'GTE', 'LT', 'LTE', 'EQ', 'IN', null].some((op) =>
				hasValueTo(op as Condition['operator'])
			)
		).toBe(false);
	});
});

describe('sortConditions', () => {
	it('puts the rejection before the yellow zone', () => {
		const underwriter: Condition = { result: 'UNDERWRITER', valueType: 'INTEGER', value: '1' };
		const rejected: Condition = { result: 'REJECTED', valueType: 'INTEGER', value: '2' };

		expect(sortConditions([underwriter, rejected])).toEqual([rejected, underwriter]);
	});
});
