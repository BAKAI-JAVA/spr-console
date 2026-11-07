import type { Condition, ConditionResult, Operator } from '#lib/api/index.js';

/** The order conditions are checked and shown in: a rejection decides before the yellow zone. */
export const RESULT_ORDER: readonly ConditionResult[] = ['REJECTED', 'UNDERWRITER'];

/** RANGE and BETWEEN have an upper bound (valueTo); the other operators have none. */
export const hasValueTo = (operator: Operator | null | undefined): boolean =>
	operator === 'RANGE' || operator === 'BETWEEN';

const FORMATS: Record<Operator, (value: string, valueTo: string) => string> = {
	GT: (v) => `x > ${v}`,
	GTE: (v) => `x ≥ ${v}`,
	LT: (v) => `x < ${v}`,
	LTE: (v) => `x ≤ ${v}`,
	EQ: (v) => `x = ${v}`,
	RANGE: (v, to) => `${v} < x ≤ ${to}`,
	BETWEEN: (v, to) => `${v} ≤ x ≤ ${to}`,
	IN: (v) => `статус ∈ ${v}`
};

/** "x > 8", "5 < x ≤ 8", "статус ∈ Closed, Sold": a condition as one formula. */
export function conditionText(
	condition: Pick<Condition, 'operator' | 'value' | 'valueTo'>
): string {
	const format = condition.operator ? FORMATS[condition.operator] : undefined;
	return format ? format(condition.value, condition.valueTo ?? '') : condition.value;
}

/** The conditions of a rule in the order they are checked. */
export function sortConditions(conditions: readonly Condition[]): Condition[] {
	return [...conditions].sort(
		(a, b) => RESULT_ORDER.indexOf(a.result) - RESULT_ORDER.indexOf(b.result)
	);
}
