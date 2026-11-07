import { ApiError } from './client';

/** What a failed call says to a person: the API's own sentences, the fallback otherwise. */
export function messageOf(
	err: unknown,
	fallback = 'Что-то пошло не так. Попробуйте ещё раз.'
): string {
	if (!(err instanceof ApiError)) return fallback;

	return messagesOf(err, fallback).join('; ');
}

/** Each sentence of a refusal: the field errors of a validation failure, or its one message. */
export function messagesOf(err: unknown, fallback = 'Что-то пошло не так.'): string[] {
	if (!(err instanceof ApiError)) return [fallback];
	if (err.errors.length > 0) return err.errors;
	return [err.message || fallback];
}
