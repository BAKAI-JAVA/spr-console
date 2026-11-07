<script lang="ts">
	import Button from './Button.svelte';

	type Props = {
		onclick: () => void;
		loading?: boolean;
		shown?: number;
		total?: number;
	};

	let { onclick, loading = false, shown, total }: Props = $props();
</script>

<div class="more">
	{#if shown !== undefined}
		<span class="count">
			{total === undefined
				? `Показано ${shown.toLocaleString('ru-RU')}`
				: `${shown.toLocaleString('ru-RU')} из ${total.toLocaleString('ru-RU')}`}
		</span>
	{/if}
	<Button variant="subtle" {loading} {onclick}>Показать ещё</Button>
</div>

<style>
	.more {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: var(--space-1);
		padding-top: var(--space-1);
	}

	.count {
		color: var(--color-text-hint);
		font-size: var(--text-sm);
	}
</style>
