<script lang="ts">
	import Alert from './Alert.svelte';

	type Props = {
		/** The sentences of a refusal (messagesOf from #lib/api/index.js); none shows nothing. */
		problems: readonly string[];
	};

	let { problems }: Props = $props();
</script>

{#if problems.length === 1}
	<Alert>{problems[0]}</Alert>
{:else if problems.length > 1}
	<Alert>
		<ul>
			{#each problems as problem (problem)}<li>{problem}</li>{/each}
		</ul>
	</Alert>
{/if}

<style>
	ul {
		margin: 0;
		padding-left: 1.1em;
	}
</style>
