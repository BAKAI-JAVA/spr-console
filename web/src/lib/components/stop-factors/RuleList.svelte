<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { Metric, Rule } from '#lib/api/index.js';
	import { Badge, List, ListItem } from '#lib/components/ui/index.js';
	import { sortConditions } from '#lib/stop-factors/conditions.js';
	import { isActiveRule, paramsText } from '#lib/stop-factors/rules.js';
	import ConditionTag from './ConditionTag.svelte';

	type Props = {
		rules: Rule[];
		metric: Metric | undefined;
		/** The buttons at the end of a rule's row. */
		actions?: Snippet<[Rule, number]>;
	};

	let { rules, metric, actions }: Props = $props();
</script>

<List bordered label="Правила">
	{#each rules as rule, index (rule.id ?? index)}
		<ListItem
			title={rule.name || `Правило ${index + 1} — название по шаблону метрики`}
			description={paramsText(rule, metric)}
		>
			{#snippet lead()}
				<span class={['number', { off: !isActiveRule(rule) }]}>{index + 1}</span>
			{/snippet}

			<span class="conditions">
				{#each sortConditions(rule.conditions) as condition (condition.result)}
					<ConditionTag {condition} />
				{:else}
					<Badge tone="warning">Нет условий: правило ничего не решает</Badge>
				{/each}
				{#if !isActiveRule(rule)}<Badge>Не проверяется</Badge>{/if}
			</span>

			{#snippet end()}
				{#if actions}<span class="actions">{@render actions(rule, index)}</span>{/if}
			{/snippet}
		</ListItem>
	{/each}
</List>

<style>
	.number {
		display: inline-grid;
		place-items: center;
		width: 28px;
		height: 28px;
		border-radius: var(--radius-sm);
		background: var(--color-secondary);
		color: var(--color-text);
		font-family: var(--font-mono);
		font-size: var(--text-sm);
		font-weight: 500;
	}

	.number.off {
		color: var(--color-text-disabled);
	}

	.conditions {
		display: flex;
		flex-wrap: wrap;
		gap: var(--space-1);
		margin-top: var(--space-1);
	}

	.actions {
		display: inline-flex;
		gap: 2px;
	}
</style>
