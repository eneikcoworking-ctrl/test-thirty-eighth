<script lang="ts">
  let spintaxMessage = "";
  let useLlmPersonalization = false;
  let fileInput: HTMLInputElement;

  function handleFileUpload(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      console.log("File selected:", target.files[0].name);
    }
  }
</script>

<section class="max-w-4xl mx-auto p-4 md:p-8 space-y-6 md:space-y-8 bg-white shadow rounded-lg border border-gray-200" aria-labelledby="campaign-setup-heading">
  <h2 id="campaign-setup-heading" class="text-2xl font-bold text-gray-900">
    Campaign Setup
  </h2>

  <!-- Lead List Upload (CSV) -->
  <div class="flex flex-col md:flex-row md:items-start gap-4">
    <div class="flex-1">
      <label for="csv-upload" class="block text-sm font-medium text-gray-700 mb-1">
        Lead Target List (.csv, .txt)
      </label>
      <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md hover:border-blue-500 transition-colors">
        <div class="space-y-1 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" stroke="currentColor" fill="none" viewBox="0 0 48 48" aria-hidden="true">
            <path d="M28 8H12a4 4 0 00-4 4v20m32-12v8m0 0v8a4 4 0 01-4 4H12a4 4 0 01-4-4v-4m32-4l-3.172-3.172a4 4 0 00-5.656 0L28 28M8 32l9.172-9.172a4 4 0 015.656 0L28 28m0 0l4 4m4-24h8m-4-4v8m-12 4h.02" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <div class="flex text-sm text-gray-600 justify-center">
            <label for="csv-upload" class="relative cursor-pointer bg-white rounded-md font-medium text-blue-600 hover:text-blue-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500">
              <span>Upload a file</span>
              <input
                id="csv-upload"
                name="csv-upload"
                type="file"
                accept=".csv, .txt"
                class="sr-only"
                bind:this={fileInput}
                on:change={handleFileUpload}
                aria-describedby="csv-upload-hint"
              >
            </label>
            <p class="pl-1">or drag and drop</p>
          </div>
          <p id="csv-upload-hint" class="text-xs text-gray-500">
            CSV up to 10MB
          </p>
        </div>
      </div>
    </div>
  </div>

  <!-- Spintax Message Template -->
  <div class="flex flex-col md:flex-row md:items-start gap-4">
    <div class="flex-1 w-full">
      <label for="spintax-template" class="block text-sm font-medium text-gray-700 mb-1">
        Spintax Message Template
      </label>
      <textarea
        id="spintax-template"
        bind:value={spintaxMessage}
        rows="5"
        class="shadow-sm focus:ring-blue-500 focus:border-blue-500 block w-full sm:text-sm border-gray-300 rounded-md p-2 border"
        placeholder="e.g. {'{Hi|Hello|Hey}'} there! We have a {'{great|special}'} offer for you."
        aria-describedby="spintax-hint"
      ></textarea>
      <p id="spintax-hint" class="mt-2 text-sm text-gray-500">
        Use {"{option1|option2}"} format to randomize text and avoid spam filters.
      </p>
    </div>
  </div>

  <!-- LLM First-Offer Personalization -->
  <div class="flex flex-col md:flex-row md:items-center gap-4 py-4 border-t border-gray-200">
    <div class="flex items-center">
      <button
        type="button"
        class="bg-gray-200 relative inline-flex flex-shrink-0 h-6 w-11 border-2 border-transparent rounded-full cursor-pointer transition-colors ease-in-out duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
        class:bg-blue-600={useLlmPersonalization}
        role="switch"
        aria-checked={useLlmPersonalization}
        on:click={() => useLlmPersonalization = !useLlmPersonalization}
        id="llm-toggle"
      >
        <span class="sr-only">Enable LLM First-Offer Personalization</span>
        <span
          aria-hidden="true"
          class="translate-x-0 pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow transform ring-0 transition ease-in-out duration-200"
          class:translate-x-5={useLlmPersonalization}
        ></span>
      </button>
      <span class="ml-3" id="llm-toggle-label">
        <span class="text-sm font-medium text-gray-900">LLM Personalization</span>
        <span class="text-sm text-gray-500 ml-1 hidden md:inline">(Dynamically rephrase offers based on lead bio)</span>
      </span>
    </div>
    <div class="md:hidden mt-1 text-sm text-gray-500">
      Dynamically rephrase offers based on lead bio.
    </div>
  </div>

  <!-- Actions -->
  <div class="flex flex-col md:flex-row md:justify-end gap-3 pt-4">
    <button type="button" class="bg-white py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 w-full md:w-auto">
      Cancel
    </button>
    <button type="button" class="bg-blue-600 py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 w-full md:w-auto">
      Save Campaign
    </button>
  </div>
</section>
