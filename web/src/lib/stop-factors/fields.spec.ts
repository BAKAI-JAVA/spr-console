import { describe, expect, it } from 'vitest';
import type { StopFactor } from '#lib/api/index.js';
import { fieldsChanged, fieldsOf, requestFields, updateRequest } from './fields';

const stopFactor: StopFactor = {
	factorId: 4,
	versionId: 40,
	versionNumber: 2,
	current: true,
	name: 'Количество реструктуризаций',
	description: null,
	active: true,
	orderNumber: 4,
	metricId: 4,
	metricTitle: 'Реструктуризации',
	calculator: 'RESTRUCTURING_COUNT',
	productIds: [3, 1],
	segment: null,
	rules: [{ id: 9, name: 'KIB', params: { source: 'KIB' }, conditions: [], active: true }],
	validFrom: '2026-01-01T00:00:00',
	validTo: null,
	changeReason: null,
	createdBy: 'admin',
	createdDate: null
};

describe('requestFields', () => {
	it('leaves empty fields to the backend and reads the order as a number', () => {
		const fields = { ...fieldsOf(stopFactor), name: ' ', description: '', orderNumber: 'x' };

		expect(requestFields(fields)).toMatchObject({
			name: null,
			description: null,
			orderNumber: null
		});
		expect(requestFields(fieldsOf(stopFactor)).orderNumber).toBe(4);
	});
});

describe('fieldsChanged', () => {
	const tests: { name: string; change: Partial<ReturnType<typeof fieldsOf>>; want: boolean }[] = [
		{ name: 'nothing', change: {}, want: false },
		{ name: 'the products in another order', change: { productIds: [1, 3] }, want: false },
		{
			name: 'trailing spaces in the name',
			change: { name: 'Количество реструктуризаций ' },
			want: false
		},
		{ name: 'the segment', change: { segment: 'MSB' }, want: true },
		{ name: 'the status', change: { active: false }, want: true }
	];

	for (const { name, change, want } of tests) {
		it(name, () =>
			expect(fieldsChanged({ ...fieldsOf(stopFactor), ...change }, stopFactor)).toBe(want)
		);
	}
});

describe('updateRequest', () => {
	it('keeps the name, the order and the rules, without the rule ids', () => {
		const fields = { ...fieldsOf(stopFactor), name: '', orderNumber: '', segment: 'MSB' as const };

		expect(updateRequest(stopFactor, fields, 'Только МСБ')).toEqual({
			metricId: 4,
			name: 'Количество реструктуризаций',
			description: null,
			segment: 'MSB',
			productIds: [1, 3],
			active: true,
			orderNumber: 4,
			rules: [{ name: 'KIB', params: { source: 'KIB' }, conditions: [], active: true }],
			changeReason: 'Только МСБ'
		});
	});
});
