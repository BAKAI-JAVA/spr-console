<script lang="ts">
	import { page } from '$app/state';
	import { resolve } from '$app/paths';
	import { pageTitle } from '#lib/brand.js';
	import { LinkButton } from '#lib/components/ui/index.js';
</script>

<svelte:head>
	<title>{pageTitle(String(page.status))}</title>
</svelte:head>

<main>
	<i class="ri-error-warning-line" aria-hidden="true"></i>
	<h1>{page.status === 404 ? 'Не найдено' : page.status}</h1>
	<p>{page.error?.message ?? 'Что-то пошло не так.'}</p>
	<LinkButton href={resolve('/(panel)/stop-factors')} variant="subtle" icon="ri-arrow-left-line">
		К стоп-факторам
	</LinkButton>
</main>

<style>
	main {
		display: flex;
		flex-direction: column;
		gap: var(--space-2);
		place-content: center;
		align-items: center;
		min-height: 100dvh;
		padding: var(--space-5);
		text-align: center;
	}

	i {
		color: var(--color-text-hint);
		font-size: 32px;
	}

	h1 {
		margin: 0;
	}

	p {
		max-width: 60ch;
		margin: 0 0 var(--space-2);
		color: var(--color-text-hint);
	}
</style>
