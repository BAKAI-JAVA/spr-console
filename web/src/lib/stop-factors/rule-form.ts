import type {
	Calculator,
	Condition,
	ConditionResult,
	Metric,
	Operator,
	Rule,
	ValueType
} from '#lib/api/index.js';
import { hasValueTo, RESULT_ORDER } from './conditions';

/** One condition as the rule sheet edits it: every value a string, turned off rather than absent. */
export type ConditionForm = {
	enabled: boolean;
	valueType: ValueType;
	operator: Operator;
	value: string;
	valueTo: string;
};

/** A rule as the rule sheet edits it. */
export type RuleForm = {
	name: string;
	active: boolean;
	params: Record<string, string>;
	conditions: Record<ConditionResult, ConditionForm>;
};

/** What the sheet needs from the stop factor's metric. */
export type RuleContext = { metric: Metric; calculator: Calculator | undefined };

/** A condition a new rule starts with: loan statuses for a list metric, a threshold for a number. */
export function defaultCondition(
	result: ConditionResult,
	calculator: Calculator | undefined,
	enabled = false
): ConditionForm {
	const list = calculator?.valueKind === 'LIST';

	return {
		enabled,
		valueType: list ? 'ARRAY' : 'INTEGER',
		operator: list ? 'IN' : result === 'REJECTED' ? 'GT' : 'RANGE',
		value: '',
		valueTo: ''
	};
}

/** The form of a rule; a new rule (none) starts with a rejection threshold to fill in. */
export function toRuleForm(rule: Rule | null, { metric, calculator }: RuleContext): RuleForm {
	const condition = (result: ConditionResult): ConditionForm => {
		const c = rule?.conditions.find((x) => x.result === result);
		if (!c) return defaultCondition(result, calculator, !rule && result === 'REJECTED');

		return {
			enabled: true,
			valueType: c.valueType,
			operator: c.operator ?? defaultCondition(result, calculator).operator,
			value: c.value,
			valueTo: c.valueTo ?? ''
		};
	};

	return {
		name: rule?.name ?? '',
		active: rule?.active ?? true,
		params: Object.fromEntries(
			(metric.params ?? []).map((p) => [p.key, rule?.params[p.key]?.toString() ?? ''])
		),
		conditions: { REJECTED: condition('REJECTED'), UNDERWRITER: condition('UNDERWRITER') }
	};
}

/**
 * The rule a form describes. An empty param is left out, a whole number of an INTEGER param is sent
 * as a number. A name left as it was is left out too: the backend keeps it while the params stay,
 * and renders it from the metric's template when they change.
 */
export function fromRuleForm(
	form: RuleForm,
	original: Rule | null,
	{ metric, calculator }: RuleContext
): Rule {
	const types = new Map((calculator?.params ?? []).map((p) => [p.key, p.type]));

	const params: Rule['params'] = {};
	for (const { key } of metric.params ?? []) {
		const raw = form.params[key]?.trim();
		if (!raw) continue;
		params[key] = types.get(key) === 'INTEGER' && /^-?\d+$/.test(raw) ? Number(raw) : raw;
	}

	const conditions: Condition[] = RESULT_ORDER.filter(
		(result) => form.conditions[result].enabled
	).map((result) => {
		const c = form.conditions[result];
		return {
			result,
			valueType: c.valueType,
			operator: c.operator,
			value: c.value.trim(),
			valueTo: hasValueTo(c.operator) ? c.valueTo.trim() : null
		};
	});

	const name = form.name.trim();

	return {
		name: name && name !== (original?.name ?? '') ? name : null,
		params,
		conditions,
		active: form.active
	};
}
