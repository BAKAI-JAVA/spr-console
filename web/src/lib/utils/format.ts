const LOCALE = 'ru-RU';

export function formatDateTime(value: string | null | undefined): string {
	if (!value) return '—';
	const date = new Date(value);

	return Number.isNaN(date.getTime())
		? value
		: date.toLocaleString(LOCALE, { dateStyle: 'medium', timeStyle: 'short' });
}

export function formatDate(value: string | null | undefined): string {
	if (!value) return '—';
	const date = new Date(value);

	return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString(LOCALE);
}

const MINUTE = 60_000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

export function formatRelative(value: string | null | undefined, now: Date = new Date()): string {
	if (!value) return '—';
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return value;

	const elapsed = now.getTime() - date.getTime();

	if (elapsed < MINUTE) return 'только что';
	if (elapsed < HOUR) return `${Math.floor(elapsed / MINUTE)} мин назад`;
	if (elapsed < DAY) return `${Math.floor(elapsed / HOUR)} ч назад`;
	if (elapsed < 2 * DAY) return 'вчера';
	if (elapsed < 7 * DAY)
		return `${Math.floor(elapsed / DAY)} ${plural(Math.floor(elapsed / DAY), 'день', 'дня', 'дней')} назад`;

	return date.toLocaleDateString(LOCALE, { day: 'numeric', month: 'short', year: 'numeric' });
}

export function initials(name: string): string {
	const words = name.split(/[\s._-]+/).filter(Boolean);
	const letters = words.length > 1 ? words[0][0] + words[1][0] : (words[0] ?? '').slice(0, 2);

	return letters.toUpperCase();
}

/** The Russian form of a noun for a count: plural(5, 'правило', 'правила', 'правил'). */
export function plural(n: number, one: string, few: string, many: string): string {
	const mod10 = n % 10;
	const mod100 = n % 100;
	if (mod10 === 1 && mod100 !== 11) return one;
	if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return few;
	return many;
}

export function countOf(n: number, one: string, few: string, many: string): string {
	return `${n.toLocaleString(LOCALE)} ${plural(n, one, few, many)}`;
}
