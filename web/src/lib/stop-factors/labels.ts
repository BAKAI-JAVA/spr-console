// What the console calls the API's codes.
import type { ConditionResult, Operator, ProductSegment, ValueType } from '#lib/api/index.js';

export const SEGMENTS: Record<ProductSegment, string> = {
	RETAIL: 'Розница и ИФЦ',
	MSB: 'МСБ'
};

export const RESULTS: Record<ConditionResult, string> = {
	REJECTED: 'Отказ',
	UNDERWRITER: 'Андеррайтер'
};

/** What a result does to an application, said beside its condition. */
export const RESULT_EFFECTS: Record<ConditionResult, string> = {
	REJECTED: 'Заявка отклоняется, если условие выполнено.',
	UNDERWRITER: 'Заявка уходит андеррайтеру, если условие выполнено.'
};

export const VALUE_TYPES: Record<ValueType, string> = {
	INTEGER: 'Целое число',
	DOUBLE: 'Число',
	ARRAY: 'Список статусов',
	STRING: 'Статус'
};

export const OPERATOR_HINTS: Record<Operator, string> = {
	GT: 'x > значение',
	GTE: 'x ≥ значение',
	LT: 'x < значение',
	LTE: 'x ≤ значение',
	EQ: 'x = значение',
	RANGE: 'значение < x ≤ до',
	BETWEEN: 'значение ≤ x ≤ до',
	IN: 'статус кредита из списка'
};

export const segmentLabel = (segment: ProductSegment | null | undefined) =>
	segment ? SEGMENTS[segment] : 'Все сегменты';

export const statusLabel = (active: boolean) => (active ? 'Активен' : 'Отключён');
