<script lang="ts">
	import type { Condition } from '#lib/api/index.js';
	import { Tag } from '#lib/components/ui/index.js';
	import { conditionText } from '#lib/stop-factors/conditions.js';
	import { RESULTS } from '#lib/stop-factors/labels.js';

	type Props = { condition: Condition };

	let { condition }: Props = $props();
</script>

<Tag tone={condition.result === 'REJECTED' ? 'danger' : 'warning'} dot small>
	{RESULTS[condition.result]}
	<span class="formula">{conditionText(condition)}</span>
</Tag>

<style>
	.formula {
		font-family: var(--font-mono);
		font-size: 0.95em;
	}
</style>
