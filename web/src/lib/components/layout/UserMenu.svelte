<script lang="ts">
	import { refreshAll } from '$app/navigation';
	import { COOKIES } from '#lib/brand.js';
	import {
		ConfirmDialog,
		DropdownMenu,
		Input,
		MenuInfo,
		MenuItem,
		MenuSeparator,
		Thumb
	} from '#lib/components/ui/index.js';
	import { rememberChoice } from '#lib/utils/cookies.js';
	import { initials } from '#lib/utils/format.js';

	type Props = { user: string };

	let { user }: Props = $props();

	let editing = $state(false);
	let draft = $state('');
	let error = $state('');

	const name = $derived(user || 'Не указан');

	function edit() {
		draft = user;
		error = '';
		editing = true;
	}

	async function save() {
		const next = draft.trim();
		if (!next) {
			error = 'Укажите логин: он сохраняется автором каждой версии';
			return;
		}
		rememberChoice(COOKIES.user, next);
		editing = false;
		await refreshAll();
	}
</script>

<DropdownMenu label="Пользователь: {name}" shape="labelled" width="16rem">
	{#snippet trigger()}
		<Thumb
			icon={user ? undefined : 'ri-user-line'}
			text={user ? initials(user) : undefined}
			size="xs"
			shape="circle"
			tone="accent"
		/>
		<span class="name">{name}</span>
		<span class="chevron" aria-hidden="true"><i class="ri-arrow-down-s-line"></i></span>
	{/snippet}

	<MenuInfo
		icon="ri-user-line"
		label={name}
		description="Автор изменений: сохраняется в истории версий"
	/>

	<MenuSeparator />

	<MenuItem
		value="user"
		icon="ri-edit-line"
		label={user ? 'Сменить пользователя' : 'Указать пользователя'}
		onSelect={edit}
	/>
</DropdownMenu>

<ConfirmDialog
	bind:open={editing}
	title="Кто вносит изменения"
	description="Логин уходит в бэкенд заголовком X-User и сохраняется автором каждой версии стоп-фактора."
	tone="info"
	icon="ri-user-line"
	confirmLabel="Сохранить"
	onConfirm={save}
>
	<Input label="Логин" bind:value={draft} {error} autocomplete="username" />
</ConfirmDialog>

<style>
	.name {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.chevron {
		display: inline-flex;
		flex: none;
		color: var(--color-text-hint);
		transition: transform var(--speed);
	}

	:global(.dropdown-trigger[data-state='open']) .chevron {
		transform: rotate(180deg);
	}

	@media (max-width: 64rem) {
		.name,
		.chevron {
			display: none;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.chevron {
			transition: none;
		}
	}
</style>
