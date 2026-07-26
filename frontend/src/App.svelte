<script>
  // State variables
  let currentStep = 1; // 1: Basics, 2: Audience, 3: Content

  // Step 1: Basics fields
  let campaignName = "Crypto Dev Outreach Q3";
  let objective = "conversion";
  let deliveryChannels = {
    email: true,
    sms: false,
    push: true
  };
  let startDate = "2024-10-12";
  let endDate = "2024-10-26";
  let budget = 500;

  // Step 2: Audience / Lead Ingestion fields
  let targetLists = [
    { id: 1, name: "Solidity Developers" },
    { id: 2, name: "Telegram Group Inbounds" }
  ];
  let selectedTargetListId = 1;
  let newTargetListName = "";
  let showCreateTargetList = false;
  let uploadedFile = null;
  let fileError = "";
  let importSuccessMessage = "";
  let parsedLeads = []; // Array of leads parsed/imported manually or via CSV

  // Manual Lead fields
  let manualLeadUsername = "";
  let manualLeadPhone = "";
  let manualLeadFirstName = "";
  let manualLeadLastName = "";
  let manualLeadMetadata = "";

  // Step 3: Content fields
  let spintaxTemplate = "{Hi|Hello|Hey} {there|friend|}, check this out!";
  let useLlmPersonalization = false;
  let campaignStatus = "DRAFT"; // DRAFT, ACTIVE, PAUSED, COMPLETED

  // Spintax balancing check
  $: spintaxValid = checkSpintaxBalance(spintaxTemplate);
  $: spintaxError = spintaxValid ? "" : "Warning: Unbalanced brackets in Spintax template.";

  function checkSpintaxBalance(text) {
    let openCount = 0;
    for (let char of text) {
      if (char === '{') openCount++;
      if (char === '}') {
        openCount--;
        if (openCount < 0) return false;
      }
    }
    return openCount === 0;
  }

  // Insert spintax variable helper
  function insertVariable(variable) {
    spintaxTemplate += ` {${variable}}`;
  }

  // Navigation handlers
  function handleNext() {
    if (currentStep < 3) {
      currentStep += 1;
    } else {
      handleSaveCampaign();
    }
  }

  // Go to step directly
  function goToStep(step) {
    currentStep = step;
  }

  function handleBack() {
    if (currentStep > 1) {
      currentStep -= 1;
    }
  }

  // Lead List Ingestion actions
  function handleCsvDrop(event) {
    event.preventDefault();
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      processCsvFile(files[0]);
    }
  }

  function handleCsvSelect(event) {
    const files = event.target.files;
    if (files && files.length > 0) {
      processCsvFile(files[0]);
    }
  }

  function processCsvFile(file) {
    if (!file.name.endsWith('.csv') && !file.name.endsWith('.txt')) {
      fileError = "Invalid file type. Please upload a .csv or .txt file.";
      uploadedFile = null;
      return;
    }
    fileError = "";
    uploadedFile = file;

    // Simulate reading CSV rows
    const reader = new FileReader();
    reader.onload = (e) => {
      const text = e.target.result;
      const lines = text.split('\n').map(l => l.trim()).filter(l => l.length > 0);
      let leadsCount = 0;
      parsedLeads = [];

      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(',').map(c => c.trim().replace(/^["']|["']$/g, ''));
        if (cols[0]) {
          parsedLeads.push({
            username: cols[0],
            phoneNumber: cols[1] || "",
            firstName: cols[2] || "",
            lastName: cols[3] || "",
            metadata: cols[4] || ""
          });
          leadsCount++;
        }
      }
      importSuccessMessage = `Successfully imported ${leadsCount} leads from ${file.name}.`;
    };
    reader.readAsText(file);
  }

  function handleAddManualLead() {
    if (!manualLeadUsername) {
      fileError = "Lead Username is required.";
      return;
    }
    fileError = "";
    parsedLeads = [
      ...parsedLeads,
      {
        username: manualLeadUsername,
        phoneNumber: manualLeadPhone,
        firstName: manualLeadFirstName,
        lastName: manualLeadLastName,
        metadata: manualLeadMetadata
      }
    ];
    manualLeadUsername = "";
    manualLeadPhone = "";
    manualLeadFirstName = "";
    manualLeadLastName = "";
    manualLeadMetadata = "";
    importSuccessMessage = "Lead added successfully to current batch.";
  }

  function handleCreateTargetList() {
    if (!newTargetListName) return;
    const newId = targetLists.length + 1;
    targetLists = [...targetLists, { id: newId, name: newTargetListName }];
    selectedTargetListId = newId;
    newTargetListName = "";
    showCreateTargetList = false;
  }

  // Create or Update Campaign
  let isSaving = false;
  let saveSuccess = false;

  async function handleSaveCampaign() {
    isSaving = true;
    saveSuccess = false;

    const payload = {
      name: campaignName,
      spintaxTemplate,
      useLlmPersonalization,
      status: campaignStatus,
      targetListId: selectedTargetListId
    };

    console.log("Saving campaign configurations:", payload);

    // Simulate API request to /api/campaigns
    setTimeout(() => {
      isSaving = false;
      saveSuccess = true;
    }, 1200);
  }
</script>

<div class="min-h-screen flex flex-col font-body-md text-body-md bg-background text-on-surface">

  <!-- Header App Bar -->
  <header class="fixed top-0 w-full bg-surface z-50 flex justify-between items-center h-16 px-md border-b border-outline-variant">
    <button aria-label="Go back" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container-low transition-colors active:scale-95">
      <span class="material-symbols-outlined text-primary">arrow_back</span>
    </button>
    <h1 class="font-headline-md text-headline-md text-primary select-none">Campaign Setup</h1>
    <button aria-label="More options" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container-low transition-colors active:scale-95">
      <span class="material-symbols-outlined text-primary">more_vert</span>
    </button>
  </header>

  <!-- Main Canvas Content -->
  <main class="flex-grow pt-20 pb-32 px-md md:px-lg max-w-4xl mx-auto w-full overflow-y-auto">

    <!-- Multi-Step Progress Stepper -->
    <nav aria-label="Progress Stepper" class="mt-4 mb-8">
      <div class="flex justify-between items-center relative py-4">
        <!-- Progress Line Background -->
        <div class="absolute h-[2px] bg-outline-variant w-full top-1/2 -translate-y-1/2 z-0"></div>
        <!-- Active Progress Line -->
        <div
          class="absolute h-[2px] bg-primary top-1/2 -translate-y-1/2 z-0 transition-all duration-500"
          style="width: {currentStep === 1 ? '16%' : currentStep === 2 ? '50%' : '100%'}">
        </div>

        <!-- Steps -->
        <button
          on:click={() => goToStep(1)}
          type="button"
          class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
          aria-current={currentStep === 1 ? "step" : undefined}>
          <div class="w-10 h-10 rounded-full flex items-center justify-center font-label-md transition-all duration-300
            {currentStep >= 1 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
            1
          </div>
          <span class="font-label-sm text-label-sm {currentStep >= 1 ? 'text-primary font-semibold' : 'text-on-surface-variant'}">Basics</span>
        </button>

        <button
          on:click={() => goToStep(2)}
          type="button"
          class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
          aria-current={currentStep === 2 ? "step" : undefined}>
          <div class="w-10 h-10 rounded-full flex items-center justify-center font-label-md transition-all duration-300
            {currentStep >= 2 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
            2
          </div>
          <span class="font-label-sm text-label-sm {currentStep >= 2 ? 'text-primary font-semibold' : 'text-on-surface-variant'}">Audience</span>
        </button>

        <button
          on:click={() => goToStep(3)}
          type="button"
          class="relative z-10 flex flex-col items-center gap-1 focus:outline-none"
          aria-current={currentStep === 3 ? "step" : undefined}>
          <div class="w-10 h-10 rounded-full flex items-center justify-center font-label-md transition-all duration-300
            {currentStep >= 3 ? 'bg-primary text-on-primary ring-4 ring-primary/20' : 'bg-surface-container-high text-on-surface-variant'}">
            3
          </div>
          <span class="font-label-sm text-label-sm {currentStep >= 3 ? 'text-primary font-semibold' : 'text-on-surface-variant'}">Content</span>
        </button>
      </div>
    </nav>

    <!-- STEP 1: CAMPAIGN BASICS -->
    {#if currentStep === 1}
      <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="basics-heading">
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
          <div class="flex items-center gap-2 mb-6">
            <span class="material-symbols-outlined text-primary">info</span>
            <h2 id="basics-heading" class="font-headline-md text-headline-md text-primary">Campaign Basics</h2>
          </div>

          <div class="space-y-6">
            <!-- Campaign Name -->
            <div class="flex flex-col gap-2">
              <label class="font-label-md text-label-md text-on-surface-variant ml-1" for="campaign-name">Campaign Name</label>
              <input
                id="campaign-name"
                bind:value={campaignName}
                class="h-12 border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all placeholder:text-outline-variant bg-white"
                placeholder="e.g. Summer Launch 2024"
                type="text"
              />
            </div>

            <!-- Objective Selection -->
            <div class="flex flex-col gap-2">
              <label class="font-label-md text-label-md text-on-surface-variant ml-1" for="objective">Campaign Objective</label>
              <div class="relative">
                <select
                  id="objective"
                  bind:value={objective}
                  class="w-full h-12 appearance-none border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none bg-white pr-10"
                >
                  <option value="awareness">Awareness & Brand Outreach</option>
                  <option value="conversion">High-Intent Lead Qualification</option>
                  <option value="retention">Customer Loyalty & Re-engagement</option>
                </select>
                <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">expand_more</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Channels toggle section -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
          <h2 class="font-label-md text-label-md text-on-surface-variant mb-4 ml-1">Delivery Channels</h2>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <!-- Email Toggle -->
            <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
              <div class="flex items-center gap-3">
                <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">email</span>
                <span class="font-body-lg text-body-lg">Email</span>
              </div>
              <input
                type="checkbox"
                bind:checked={deliveryChannels.email}
                class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
              />
            </label>

            <!-- SMS Toggle -->
            <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
              <div class="flex items-center gap-3">
                <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">sms</span>
                <span class="font-body-lg text-body-lg">SMS</span>
              </div>
              <input
                type="checkbox"
                bind:checked={deliveryChannels.sms}
                class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
              />
            </label>

            <!-- Push Toggle -->
            <label class="flex items-center justify-between p-4 border border-outline-variant rounded-lg hover:bg-surface-container-low transition-colors cursor-pointer group">
              <div class="flex items-center gap-3">
                <span class="material-symbols-outlined text-on-surface-variant group-hover:text-primary transition-colors">notifications_active</span>
                <span class="font-body-lg text-body-lg">Push</span>
              </div>
              <input
                type="checkbox"
                bind:checked={deliveryChannels.push}
                class="w-10 h-5 rounded-full bg-outline-variant checked:bg-primary appearance-none relative cursor-pointer transition-colors before:content-[''] before:absolute before:w-4 before:h-4 before:bg-white before:rounded-full before:top-0.5 before:left-0.5 checked:before:translate-x-5 before:transition-transform"
              />
            </label>
          </div>
        </div>

        <!-- Schedule & Budget Section -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <!-- Date Pickers -->
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
            <h2 class="font-label-md text-label-md text-on-surface-variant mb-6 ml-1">Schedule Range</h2>
            <div class="flex flex-col gap-4">
              <div class="relative">
                <label for="start-date" class="absolute -top-2 left-3 bg-white px-1 font-label-sm text-label-sm text-primary">Start Date</label>
                <div class="h-12 border border-primary border-2 rounded-lg flex items-center px-4 justify-between bg-white">
                  <input id="start-date" type="date" bind:value={startDate} class="w-full bg-transparent border-none outline-none font-body-md text-body-md focus:ring-0" />
                </div>
              </div>
              <div class="relative mt-2">
                <label for="end-date" class="absolute -top-2 left-3 bg-white px-1 font-label-sm text-label-sm text-on-surface-variant">End Date</label>
                <div class="h-12 border border-outline-variant rounded-lg flex items-center px-4 justify-between bg-white">
                  <input id="end-date" type="date" bind:value={endDate} class="w-full bg-transparent border-none outline-none font-body-md text-body-md focus:ring-0" />
                </div>
              </div>
            </div>
          </div>

          <!-- Budget Selection -->
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
            <h2 class="font-label-md text-label-md text-on-surface-variant mb-6 ml-1">Campaign Budget</h2>
            <div class="relative">
              <span class="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant font-body-lg text-body-lg">$</span>
              <input
                class="w-full h-12 border border-outline-variant rounded-lg pl-8 pr-12 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all font-body-lg text-body-lg bg-white"
                placeholder="0.00"
                type="number"
                bind:value={budget}
              />
              <div class="absolute right-3 top-1/2 -translate-y-1/2">
                <span class="font-label-sm text-label-sm text-on-surface-variant font-semibold">USD</span>
              </div>
            </div>
            <p class="mt-4 text-on-surface-variant font-label-sm text-label-sm flex items-center gap-1">
              <span class="material-symbols-outlined text-[14px]">trending_up</span>
              Recommended minimum: $500.00
            </p>
          </div>
        </div>

        <!-- Helpful Hint banner -->
        <div class="p-4 bg-secondary-container/10 border border-secondary/20 rounded-xl flex gap-3 items-start">
          <span class="material-symbols-outlined text-secondary">lightbulb</span>
          <p class="text-on-secondary-fixed-variant font-body-md text-body-md">
            Targeting <strong>Retainable Audience</strong> can increase your conversion rate by up to 15% for this objective.
          </p>
        </div>
      </section>
    {/if}

    <!-- STEP 2: AUDIENCE / LEAD INGESTION -->
    {#if currentStep === 2}
      <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="audience-heading">
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
          <div class="flex items-center gap-2 mb-6">
            <span class="material-symbols-outlined text-primary">groups</span>
            <h2 id="audience-heading" class="font-headline-md text-headline-md text-primary">Target Audience Configuration</h2>
          </div>

          <!-- Select or Create Target List -->
          <div class="space-y-4 mb-6">
            <div class="flex justify-between items-center">
              <label class="font-label-md text-label-md text-on-surface-variant" for="target-list-select">Choose Target List</label>
              <button
                on:click={() => showCreateTargetList = !showCreateTargetList}
                class="text-sm font-semibold text-secondary hover:underline flex items-center gap-1 focus:outline-none"
              >
                <span class="material-symbols-outlined text-xs">add</span> Create New List
              </button>
            </div>

            {#if showCreateTargetList}
              <div class="p-4 border border-outline-variant bg-surface-container-low rounded-lg flex gap-3 items-center animate-fadeIn">
                <input
                  type="text"
                  placeholder="Target List Name (e.g. DeFi Whales)"
                  bind:value={newTargetListName}
                  class="flex-grow h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
                />
                <button
                  on:click={handleCreateTargetList}
                  class="h-10 px-4 bg-primary text-on-primary rounded-lg font-label-md hover:bg-primary/95 transition-all text-xs"
                >
                  Save
                </button>
              </div>
            {/if}

            <div class="relative">
              <select
                id="target-list-select"
                bind:value={selectedTargetListId}
                class="w-full h-12 appearance-none border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary bg-white pr-10"
              >
                {#each targetLists as list}
                  <option value={list.id}>{list.name}</option>
                {/each}
              </select>
              <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">expand_more</span>
            </div>
          </div>
        </div>

        <!-- Drag and Drop CSV Ingestion -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
          <h3 class="font-label-md text-label-md text-on-surface-variant mb-3 ml-1">Import Leads via CSV / TXT</h3>

          <div
            on:dragover|preventDefault
            on:drop={handleCsvDrop}
            role="region"
            aria-label="File Upload Area"
            class="border-2 border-dashed border-outline-variant rounded-lg p-8 flex flex-col items-center justify-center bg-surface-container-low hover:bg-surface-container-low/80 hover:border-primary transition-all cursor-pointer relative"
          >
            <input
              type="file"
              accept=".csv,.txt"
              on:change={handleCsvSelect}
              class="absolute inset-0 opacity-0 cursor-pointer"
              id="csv-file-input"
            />
            <span class="material-symbols-outlined text-outline text-4xl mb-2">upload_file</span>
            <p class="font-body-lg text-body-lg text-on-surface-variant text-center">
              Drag and drop your <strong>.csv</strong> or <strong>.txt</strong> file here
            </p>
            <p class="text-xs text-outline mt-1 text-center">
              Columns expected: username, phone_number, first_name, last_name, metadata
            </p>
          </div>

          {#if fileError}
            <div class="p-3 bg-error-container text-error rounded-lg text-xs mt-3 flex items-center gap-2">
              <span class="material-symbols-outlined text-sm">error</span>
              {fileError}
            </div>
          {/if}

          {#if uploadedFile}
            <div class="p-3 bg-[#e8f5e9] text-[#2e7d32] border border-[#a5d6a7] rounded-lg text-xs mt-3 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="material-symbols-outlined text-sm">check_circle</span>
                <span>Uploaded: <strong>{uploadedFile.name}</strong> ({Math.round(uploadedFile.size / 1024)} KB)</span>
              </div>
              <button on:click={() => {uploadedFile = null; importSuccessMessage = ""; parsedLeads = [];}} class="text-[#2e7d32] hover:underline focus:outline-none">Remove</button>
            </div>
          {/if}
        </div>

        <!-- Manual Batch Entry -->
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
          <h3 class="font-label-md text-label-md text-on-surface-variant mb-3 ml-1">Or Add Lead Manually</h3>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <input
              type="text"
              placeholder="@username (Required)"
              bind:value={manualLeadUsername}
              class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
            />
            <input
              type="text"
              placeholder="Phone number"
              bind:value={manualLeadPhone}
              class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
            />
            <input
              type="text"
              placeholder="First Name"
              bind:value={manualLeadFirstName}
              class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
            />
            <input
              type="text"
              placeholder="Last Name"
              bind:value={manualLeadLastName}
              class="h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
            />
            <input
              type="text"
              placeholder="Metadata (e.g. bio context)"
              bind:value={manualLeadMetadata}
              class="md:col-span-2 h-10 border border-outline-variant rounded-lg px-3 focus:ring-2 focus:ring-primary bg-white outline-none"
            />
          </div>

          <button
            on:click={handleAddManualLead}
            class="mt-4 h-10 px-6 bg-secondary text-on-primary rounded-lg font-label-md hover:bg-secondary/95 transition-all w-full md:w-auto"
          >
            Add to Batch
          </button>
        </div>

        <!-- Current Batch Preview -->
        {#if parsedLeads.length > 0}
          <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm animate-fadeIn">
            <h3 class="font-label-md text-label-md text-on-surface-variant mb-3 ml-1">Current Batch Preview ({parsedLeads.length} leads)</h3>
            <div class="overflow-x-auto max-h-60 custom-scrollbar">
              <table class="w-full text-left border-collapse">
                <thead>
                  <tr class="border-b border-outline-variant bg-surface-container-low text-xs font-semibold text-on-surface-variant">
                    <th class="p-3">Username</th>
                    <th class="p-3">Phone</th>
                    <th class="p-3">First Name</th>
                    <th class="p-3">Last Name</th>
                  </tr>
                </thead>
                <tbody>
                  {#each parsedLeads as lead}
                    <tr class="border-b border-outline-variant/50 text-xs">
                      <td class="p-3 font-semibold text-primary">{lead.username}</td>
                      <td class="p-3 text-on-surface-variant">{lead.phoneNumber || "—"}</td>
                      <td class="p-3">{lead.firstName || "—"}</td>
                      <td class="p-3">{lead.lastName || "—"}</td>
                    </tr>
                  {/each}
                </tbody>
              </table>
            </div>
          </div>
        {/if}
      </section>
    {/if}

    <!-- STEP 3: CONTENT & SPINTAX TEMPLATE -->
    {#if currentStep === 3}
      <section class="flex flex-col gap-6 animate-fadeIn" aria-labelledby="content-heading">
        <div class="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm">
          <div class="flex items-center gap-2 mb-6">
            <span class="material-symbols-outlined text-primary">chat</span>
            <h2 id="content-heading" class="font-headline-md text-headline-md text-primary">Outreach Content & Spintax</h2>
          </div>

          <div class="space-y-6">
            <!-- Spintax Template Editor -->
            <div class="flex flex-col gap-2">
              <div class="flex justify-between items-center ml-1">
                <label class="font-label-md text-label-md text-on-surface-variant" for="spintax-editor">Spintax Message Template</label>
                <div class="flex gap-2">
                  <span class="text-xs text-on-surface-variant">Insert:</span>
                  <button on:click={() => insertVariable("first_name")} class="text-xs text-primary font-semibold hover:underline focus:outline-none">{"{first_name}"}</button>
                  <button on:click={() => insertVariable("username")} class="text-xs text-primary font-semibold hover:underline focus:outline-none">{"{username}"}</button>
                </div>
              </div>

              <div class="relative">
                <textarea
                  id="spintax-editor"
                  bind:value={spintaxTemplate}
                  class="w-full h-36 border border-outline-variant rounded-lg p-4 focus:ring-2 focus:ring-primary focus:border-primary outline-none transition-all font-mono text-sm bg-white resize-none"
                  placeholder="Template syntax example: &#123;Hi|Hello&#125; &#123;username&#125;"
                ></textarea>
              </div>

              {#if spintaxError}
                <div class="p-3 bg-error-container text-error rounded-lg text-xs flex items-center gap-2 animate-fadeIn">
                  <span class="material-symbols-outlined text-sm">warning</span>
                  {spintaxError}
                </div>
              {/if}
            </div>

            <!-- AI LLM Personalized First Offer Toggle -->
            <div class="p-4 border border-outline-variant rounded-lg bg-surface-container-low flex items-start gap-4 hover:border-primary transition-all">
              <div class="pt-1">
                <input
                  type="checkbox"
                  bind:checked={useLlmPersonalization}
                  id="llm-toggle"
                  class="w-5 h-5 rounded border-outline-variant text-primary focus:ring-primary focus:ring-2 cursor-pointer"
                />
              </div>
              <div class="flex flex-col gap-1 cursor-pointer">
                <label for="llm-toggle" class="font-body-lg text-body-lg font-semibold text-primary cursor-pointer">
                  AI-Powered First-Offer Personalization
                </label>
                <p class="text-xs text-on-surface-variant">
                  When enabled, LeadGen Bot uses an LLM to dynamically rewrite your spintax outreach offer based on public bio information or custom metadata gathered from the lead's profile.
                </p>
              </div>
            </div>

            <!-- Campaign Status Select -->
            <div class="flex flex-col gap-2">
              <label class="font-label-md text-label-md text-on-surface-variant ml-1" for="campaign-status">Initial Launch Status</label>
              <div class="relative">
                <select
                  id="campaign-status"
                  bind:value={campaignStatus}
                  class="w-full h-12 appearance-none border border-outline-variant rounded-lg px-4 focus:ring-2 focus:ring-primary bg-white pr-10"
                >
                  <option value="DRAFT">Draft Mode (Save only)</option>
                  <option value="ACTIVE">Active (Launch immediately)</option>
                  <option value="PAUSED">Paused</option>
                </select>
                <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none text-on-surface-variant">expand_more</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Success Banner -->
        {#if saveSuccess}
          <div class="p-4 bg-[#e8f5e9] border border-[#a5d6a7] text-[#1b5e20] rounded-xl flex gap-3 items-center animate-fadeIn" role="alert">
            <span class="material-symbols-outlined text-[#2e7d32]">verified</span>
            <div>
              <h4 class="font-bold text-sm">Campaign Saved Successfully!</h4>
              <p class="text-xs">Your campaign configurations have been persisted and are ready to run.</p>
            </div>
          </div>
        {/if}
      </section>
    {/if}

  </main>

  <!-- Persistent Bottom Action Bar Footer -->
  <footer class="fixed bottom-0 w-full bg-surface-container-lowest px-md py-4 border-t border-outline-variant z-50">
    <div class="flex gap-4 max-w-lg mx-auto">
      {#if currentStep > 1}
        <button
          on:click={handleBack}
          class="flex-1 h-12 bg-white border border-outline-variant rounded-lg text-on-surface-variant font-label-md hover:bg-surface-container-low transition-colors active:scale-95 flex items-center justify-center gap-1"
        >
          <span class="material-symbols-outlined text-[18px]">arrow_back</span>
          Back
        </button>
      {/if}

      <button
        on:click={handleNext}
        disabled={isSaving}
        class="flex-[2] h-12 bg-primary text-on-primary rounded-lg font-label-md shadow-md shadow-primary/20 hover:bg-primary/95 transition-all flex items-center justify-center gap-2 active:scale-95 disabled:opacity-50"
      >
        {#if isSaving}
          <span class="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
          Processing...
        {:else if currentStep < 3}
          Continue
          <span class="material-symbols-outlined text-[18px]">arrow_forward</span>
        {:else}
          Save & Launch Campaign
          <span class="material-symbols-outlined text-[18px]">rocket_launch</span>
        {/if}
      </button>
    </div>
  </footer>

</div>

<style>
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .animate-fadeIn {
    animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
</style>
