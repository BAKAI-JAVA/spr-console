import { describe, expect, it } from 'vitest';
import type { Calculator, Metric, Rule } from '#lib/api/index.js';
import { fromRuleForm, toRuleForm, type RuleContext } from './rule-form';

const metric: Metric = {
	id: 10,
	title: 'Непрерывная просроченная задолженность',
	calculator: 'OVERDUE_BUCKET_COUNT',
	nameTemplate: 'с {periodFromMonth} по {periodToMonth} месяц',
	params: [
		{ key: 'periodFromMonth', label: 'Период с', required: true },
		{ key: 'periodToMonth', label: 'Период по', required: true },
		{ key: 'maxOverdueDays', label: 'Просрочка до', required: false }
	],
	active: true
};

const numbers: Calculator = {
	type: 'OVERDUE_BUCKET_COUNT',
	name: 'Просрочки',
	valueKind: 'NUMBER',
	allowedConditionValueTypes: ['INTEGER', 'DOUBLE'],
	allowedOperators: [],
	params: [
		{
			key: 'periodFromMonth',
			label: '',
			required: true,
			type: 'INTEGER',
			min: 1,
			allowedValues: null
		},
		{
			key: 'periodToMonth',
			label: '',
			required: true,
			type: 'INTEGER',
			min: 1,
			allowedValues: null
		},
		{
			key: 'maxOverdueDays',
			label: '',
			required: false,
			type: 'INTEGER',
			min: 1,
			allowedValues: null
		}
	]
};

const context: RuleContext = { metric, calculator: numbers };

const rule: Rule = {
	id: 7,
	name: 'с 7 по 12 месяц',
	params: { periodFromMonth: 7, periodToMonth: 12 },
	conditions: [
		{ result: 'REJECTED', valueType: 'INTEGER', value: '8', operator: 'GT', valueTo: null },
		{ result: 'UNDERWRITER', valueType: 'INTEGER', value: '5', operator: 'RANGE', valueTo: '8' }
	],
	active: true
};

describe('toRuleForm', () => {
	it('starts a new rule with a rejection threshold to fill in', () => {
		const form = toRuleForm(null, context);

		expect(form.conditions.REJECTED).toEqual({
			enabled: true,
			valueType: 'INTEGER',
			operator: 'GT',
			value: '',
			valueTo: ''
		});
		expect(form.conditions.UNDERWRITER.enabled).toBe(false);
		expect(form.params).toEqual({ periodFromMonth: '', periodToMonth: '', maxOverdueDays: '' });
	});

	it('starts a rule of a list metric with loan statuses', () => {
		const form = toRuleForm(null, { metric, calculator: { ...numbers, valueKind: 'LIST' } });

		expect(form.conditions.REJECTED).toMatchObject({ valueType: 'ARRAY', operator: 'IN' });
	});
});

describe('fromRuleForm', () => {
	it('gives back an unchanged rule without its name, which the backend keeps', () => {
		const back = fromRuleForm(toRuleForm(rule, context), rule, context);

		expect(back).toEqual({ ...rule, id: undefined, name: null });
	});

	it('sends a new name', () => {
		const form = toRuleForm(rule, context);
		form.name = '  Второе полугодие ';

		expect(fromRuleForm(form, rule, context).name).toBe('Второе полугодие');
	});

	it('leaves out an empty param and sends a whole number as a number', () => {
		const form = toRuleForm(rule, context);
		form.params.periodFromMonth = ' 1 ';
		form.params.maxOverdueDays = '';

		expect(fromRuleForm(form, rule, context).params).toEqual({
			periodFromMonth: 1,
			periodToMonth: 12
		});
	});

	it('drops a turned-off condition and the upper bound of a threshold', () => {
		const form = toRuleForm(rule, context);
		form.conditions.UNDERWRITER.enabled = false;
		form.conditions.REJECTED.valueTo = '99';

		expect(fromRuleForm(form, rule, context).conditions).toEqual([
			{ result: 'REJECTED', valueType: 'INTEGER', value: '8', operator: 'GT', valueTo: null }
		]);
	});
});
