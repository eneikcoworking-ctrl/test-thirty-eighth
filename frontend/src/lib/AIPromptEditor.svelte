<script>
  let temperature = 0.7;
  let maxTokens = 2048;
  let nucleusSampling = false;
  let promptText = `You are a helpful assistant specialized in technical documentation. \nAlways respond in a concise, professional tone. \n\nCONTEXT:\nUser Name: {{user_name}}\nInteraction History: {{history}}\n\nOBJECTIVE:\nSynthesize technical data provided into readable summaries.`;
  let editorRef;

  function insertVar(variable) {
    if (!editorRef) return;
    const start = editorRef.selectionStart;
    const end = editorRef.selectionEnd;
    const text = editorRef.value;
    promptText = text.substring(0, start) + variable + text.substring(end);

    // Using setTimeout to allow DOM to update before setting selection
    setTimeout(() => {
      editorRef.focus();
      editorRef.setSelectionRange(start + variable.length, start + variable.length);
    }, 0);
  }

  function toggleSwitch() {
    nucleusSampling = !nucleusSampling;
  }

  // Haptic feedback simulation
  function triggerHaptic() {
    if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
      navigator.vibrate(5);
    }
  }
</script>

<style>
  /* Custom scrollbar for textarea */
  .custom-scrollbar::-webkit-scrollbar {
    width: 6px;
  }
  .custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
  }
  .custom-scrollbar::-webkit-scrollbar-thumb {
    background-color: #4b5563; /* Tailwind gray-600 */
    border-radius: 10px;
  }

  /* Range input styling */
  input[type="range"] {
    -webkit-appearance: none;
    appearance: none;
    width: 100%;
    height: 4px;
    background: #e5e7eb; /* Tailwind gray-200 */
    border-radius: 2px;
    outline: none;
    margin: 12px 0;
  }

  :global(.dark) input[type="range"] {
    background: #374151; /* Tailwind gray-700 */
  }

  input[type="range"]::-webkit-slider-thumb {
    -webkit-appearance: none;
    appearance: none;
    width: 16px;
    height: 16px;
    background: #c0c1ff;
    border-radius: 50%;
    cursor: pointer;
    transition: transform 0.1s ease-in-out;
  }

  input[type="range"]::-moz-range-thumb {
    width: 16px;
    height: 16px;
    background: #c0c1ff;
    border-radius: 50%;
    cursor: pointer;
    transition: transform 0.1s ease-in-out;
    border: none;
  }

  input[type="range"]::-webkit-slider-thumb:hover {
    transform: scale(1.2);
  }

  input[type="range"]::-moz-range-thumb:hover {
    transform: scale(1.2);
  }

  .wrapper {
      min-height: 0;
  }
</style>

<div class="wrapper flex flex-col bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden">
  <!-- TopAppBar -->
  <header class="bg-white dark:bg-gray-900 w-full border-b border-gray-200 dark:border-gray-800">
    <div class="flex items-center justify-between px-4 py-2 w-full max-w-7xl mx-auto h-14">
      <button aria-label="Go back" class="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 active:opacity-80 transition-colors flex items-center justify-center min-w-[44px] min-h-[44px] focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2 dark:focus:ring-offset-gray-900" on:click={triggerHaptic}>
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
      </button>
      <h1 class="text-xl font-semibold">AI Prompt Editor</h1>
      <button class="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded text-sm font-medium uppercase tracking-wider active:opacity-80 transition-colors min-w-[44px] min-h-[44px] focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900" on:click={triggerHaptic}>
        Save
      </button>
    </div>
  </header>

  <!-- Main Content Canvas -->
  <main class="flex-grow py-6 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">
    <!-- Hero Identity Area (Atmospheric) -->
    <div class="relative w-full h-24 rounded-xl overflow-hidden my-6 flex items-center justify-center border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800/50">
      <div class="relative z-10 text-center">
        <span class="text-xs text-gray-500 dark:text-gray-400 uppercase tracking-widest block mb-1 font-bold">Advanced Mode</span>
        <p class="text-sm text-gray-600 dark:text-gray-300">System-level instruction tuning</p>
      </div>
    </div>

    <div class="space-y-8">
      <!-- System Prompt Area -->
      <section class="flex flex-col gap-2">
        <div class="flex items-center justify-between">
          <h2 class="text-xs font-bold text-gray-600 dark:text-gray-300 flex items-center gap-1 uppercase tracking-wider">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="4 17 10 11 4 5"></polyline><line x1="12" y1="19" x2="20" y2="19"></line></svg>
            SYSTEM PROMPT
          </h2>
          <span class="text-[10px] font-mono text-gray-400 uppercase">Read-only for users</span>
        </div>
        <div class="relative rounded-xl bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 focus-within:border-blue-500 transition-colors overflow-hidden">
          <textarea
            aria-label="System prompt editor"
            bind:this={editorRef}
            bind:value={promptText}
            class="w-full h-64 p-4 bg-transparent border-none focus:ring-0 font-mono text-sm text-gray-800 dark:text-gray-200 resize-none custom-scrollbar outline-none"
            placeholder="Define the behavior of the AI agent... Use {`{{variable}}`} syntax for dynamic inputs."
          ></textarea>
          <!-- Line Numbers / Visual Flourish -->
          <div class="absolute bottom-2 right-2 flex items-center gap-2">
            <span class="text-[10px] font-mono text-gray-400">Ln 12, Col 42</span>
            <div class="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse"></div>
          </div>
        </div>
      </section>

      <!-- Dynamic Variables Bento -->
      <section class="space-y-2">
        <h2 class="text-xs font-bold text-gray-600 dark:text-gray-300 flex items-center gap-1 uppercase tracking-wider">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M12 16v-4"></path><path d="M12 8h.01"></path></svg>
          INJECTABLE VARIABLES
        </h2>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-2">
          <button aria-label="Insert user_name variable" class="p-3 flex flex-col items-start gap-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 active:scale-95 transition-all min-h-[44px] focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" on:click={() => { insertVar('{{user_name}}'); triggerHaptic(); }}>
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{"{{user_name}}"}</span>
            <span class="text-[10px] text-gray-500 dark:text-gray-400">User Profile Name</span>
          </button>
          <button aria-label="Insert history variable" class="p-3 flex flex-col items-start gap-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 active:scale-95 transition-all min-h-[44px] focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" on:click={() => { insertVar('{{history}}'); triggerHaptic(); }}>
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{"{{history}}"}</span>
            <span class="text-[10px] text-gray-500 dark:text-gray-400">Last 5 interactions</span>
          </button>
          <button aria-label="Insert current_date variable" class="p-3 flex flex-col items-start gap-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 active:scale-95 transition-all min-h-[44px] focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" on:click={() => { insertVar('{{current_date}}'); triggerHaptic(); }}>
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{"{{current_date}}"}</span>
            <span class="text-[10px] text-gray-500 dark:text-gray-400">ISO Timestamp</span>
          </button>
          <button aria-label="Insert metadata variable" class="p-3 flex flex-col items-start gap-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 active:scale-95 transition-all min-h-[44px] focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent" on:click={() => { insertVar('{{metadata}}'); triggerHaptic(); }}>
            <span class="font-mono text-xs text-blue-600 dark:text-blue-400">{"{{metadata}}"}</span>
            <span class="text-[10px] text-gray-500 dark:text-gray-400">Session context</span>
          </button>
        </div>
      </section>

      <!-- Configuration Parameters -->
      <section class="bg-gray-50 dark:bg-gray-800/80 border border-gray-200 dark:border-gray-700 rounded-xl p-4 space-y-6">
        <h2 class="text-xs font-bold text-gray-900 dark:text-white flex items-center gap-1 uppercase tracking-wider">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>
          MODEL PARAMETERS
        </h2>

        <!-- Temperature Slider -->
        <div class="space-y-1">
          <div class="flex justify-between items-center">
            <label for="temp-slider" class="text-sm text-gray-600 dark:text-gray-300">Temperature</label>
            <span class="font-mono text-blue-600 dark:text-blue-400">{temperature.toFixed(1)}</span>
          </div>
          <input
            id="temp-slider"
            type="range"
            min="0" max="2" step="0.1"
            bind:value={temperature}
            aria-label="Temperature slider"
            on:input={triggerHaptic}
          />
          <div class="flex justify-between text-[10px] text-gray-400 uppercase">
            <span>Precise</span>
            <span>Creative</span>
          </div>
        </div>

        <div class="h-px bg-gray-200 dark:bg-gray-700 w-full"></div>

        <!-- Max Tokens Input -->
        <div class="flex items-center justify-between">
          <div class="space-y-1">
            <label for="max-tokens" class="text-sm text-gray-600 dark:text-gray-300 block">Max Tokens</label>
            <p class="text-[10px] text-gray-400">Limit output length</p>
          </div>
          <div class="flex items-center bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded px-2 py-1">
            <input
              id="max-tokens"
              type="number"
              bind:value={maxTokens}
              class="bg-transparent border-none text-right font-mono text-blue-600 dark:text-blue-400 w-16 focus:ring-0 p-0 outline-none"
              aria-label="Max tokens limit"
            />
            <span class="text-[10px] text-gray-400 ml-2">TK</span>
          </div>
        </div>

        <div class="h-px bg-gray-200 dark:bg-gray-700 w-full"></div>

        <!-- Top P Toggle -->
        <div class="flex items-center justify-between">
          <div class="space-y-1">
            <label for="nucleus-sampling-toggle" class="text-sm text-gray-600 dark:text-gray-300 block">Nucleus Sampling</label>
            <p class="text-[10px] text-gray-400">Top P filtering</p>
          </div>
          <button
            id="nucleus-sampling-toggle"
            role="switch"
            aria-checked={nucleusSampling}
            aria-label="Toggle nucleus sampling"
            class="relative inline-flex items-center cursor-pointer min-w-[44px] min-h-[44px] justify-end focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 rounded-full"
            on:click={() => { toggleSwitch(); triggerHaptic(); }}
          >
            <span class="sr-only">Toggle Nucleus Sampling</span>
            <div class="w-10 h-5 {nucleusSampling ? 'bg-blue-200 dark:bg-blue-900' : 'bg-gray-300 dark:bg-gray-600'} rounded-full transition-colors flex items-center">
               <div class="bg-blue-600 dark:bg-blue-400 w-3 h-3 rounded-full transition-transform transform {nucleusSampling ? 'translate-x-6' : 'translate-x-1'} shadow-sm"></div>
            </div>
          </button>
        </div>
      </section>


      <!-- FAQs & Qualification Rules -->
      <section class="space-y-4">
        <h2 class="text-xs font-bold text-gray-600 dark:text-gray-300 flex items-center gap-1 uppercase tracking-wider">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
          FAQS & QUALIFICATION RULES
        </h2>

        <div class="space-y-4">
          <div class="relative rounded-xl bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 focus-within:border-blue-500 transition-colors overflow-hidden">
            <label for="faqs-editor" class="sr-only">FAQs Editor</label>
            <textarea
              id="faqs-editor"
              class="w-full h-32 p-4 bg-transparent border-none focus:ring-0 font-mono text-sm text-gray-800 dark:text-gray-200 resize-none custom-scrollbar outline-none"
              placeholder="Q: What is the pricing? A: It starts at $10/mo..."
            ></textarea>
          </div>

          <div class="relative rounded-xl bg-gray-50 dark:bg-gray-800/50 border border-gray-200 dark:border-gray-700 focus-within:border-blue-500 transition-colors overflow-hidden">
            <label for="qual-editor" class="sr-only">Qualification Rules Editor</label>
            <textarea
              id="qual-editor"
              class="w-full h-32 p-4 bg-transparent border-none focus:ring-0 font-mono text-sm text-gray-800 dark:text-gray-200 resize-none custom-scrollbar outline-none"
              placeholder="Rule 1: Lead must have a budget > $500..."
            ></textarea>
          </div>
        </div>
      </section>

      <!-- Bottom CTA Section (Optional Mobile Specific) -->
      <div class="grid grid-cols-2 gap-4 pt-4 md:hidden">
        <button class="flex items-center justify-center gap-1 py-2 border border-gray-200 dark:border-gray-700 rounded-lg text-xs font-bold text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors uppercase tracking-wider min-h-[44px]" on:click={triggerHaptic}>
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v5h5"></path><path d="M3.05 13A9 9 0 1 0 6 5.3L3 8"></path><polyline points="12 7 12 12 15 15"></polyline></svg>
          HISTORY
        </button>
        <button class="flex items-center justify-center gap-1 py-2 border border-gray-200 dark:border-gray-700 rounded-lg text-xs font-bold text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors uppercase tracking-wider min-h-[44px]" on:click={triggerHaptic}>
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 10a.5.5 0 0 0 0 1v3a.5.5 0 0 0 1 0v-3a.5.5 0 0 0 0-1M14 10a.5.5 0 0 0 0 1v3a.5.5 0 0 0 1 0v-3a.5.5 0 0 0 0-1"></path><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Z"></path><path d="M8 17a6 6 0 0 0 8 0"></path></svg>
          TEST
        </button>
      </div>
    </div>
  </main>

  <!-- BottomNavBar -->
  <nav class="hidden">
    <button aria-label="Editor tab" class="flex flex-col items-center justify-center text-blue-600 dark:text-blue-400 font-bold scale-95 duration-100 min-w-[44px] min-h-[44px]" on:click={triggerHaptic}>
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
      <span class="text-[10px] uppercase tracking-wider mt-1">Editor</span>
    </button>
    <button aria-label="Test Sandbox tab" class="flex flex-col items-center justify-center text-gray-500 dark:text-gray-400 hover:text-blue-500 dark:hover:text-blue-300 cursor-pointer transition-colors min-w-[44px] min-h-[44px]" on:click={triggerHaptic}>
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 10a.5.5 0 0 0 0 1v3a.5.5 0 0 0 1 0v-3a.5.5 0 0 0 0-1M14 10a.5.5 0 0 0 0 1v3a.5.5 0 0 0 1 0v-3a.5.5 0 0 0 0-1"></path><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2Z"></path><path d="M8 17a6 6 0 0 0 8 0"></path></svg>
      <span class="text-[10px] uppercase tracking-wider mt-1">Test Sandbox</span>
    </button>
  </nav>
</div>
