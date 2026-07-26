<script>
  import { onMount } from 'svelte';

  // State
  let conversations = [];
  let selectedId = null;
  let replyText = '';
  let isLoading = true;
  let searchQuery = '';
  let activeFilter = 'All'; // 'All', 'Active', 'Waiting'
  let isMobileView = false;
  let showMobileChat = false;

  // Track window size for layout responsiveness
  function handleResize() {
    isMobileView = window.innerWidth < 768;
  }

  // Fetch all conversations from backend API
  async function fetchConversations() {
    try {
      isLoading = true;
      const res = await fetch('/api/conversations');
      if (res.ok) {
        conversations = await res.json();
      } else {
        console.error('Failed to fetch conversations');
      }
    } catch (err) {
      console.error('API Error, using fallback client-side state', err);
      // Fallback local mock data if server is unreachable
      conversations = [
        {
          id: "marcus-chen-id",
          leadUsername: "Marcus Chen",
          leadPhone: "+1234567890",
          aiState: "Stellar Dynamics",
          status: "WAITING",
          unreadCount: 3,
          updatedAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
          messages: [
            { id: "1", senderType: "LEAD", content: "Hello, I have a question regarding Stellar Dynamics integrations.", createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString() },
            { id: "2", senderType: "AI", content: "Sure Marcus! Our API supports rapid scaling. What specifically do you need?", createdAt: new Date(Date.now() - 25 * 60 * 1000).toISOString() },
            { id: "3", senderType: "LEAD", content: "We're seeing a delay in the API response times...", createdAt: new Date(Date.now() - 2 * 60 * 1000).toISOString() }
          ]
        },
        {
          id: "elena-rodriguez-id",
          leadUsername: "Elena Rodriguez",
          leadPhone: "+1987654321",
          aiState: "Horizon Logistics",
          status: "WAITING",
          unreadCount: 0,
          updatedAt: new Date(Date.now() - 14 * 60 * 1000).toISOString(),
          messages: [
            { id: "4", senderType: "LEAD", content: "The tracking number provided isn't updating yet.", createdAt: new Date(Date.now() - 14 * 60 * 1000).toISOString() }
          ]
        },
        {
          id: "jordan-smith-id",
          leadUsername: "Jordan Smith",
          leadPhone: "+1555555555",
          aiState: "CloudScale Inc.",
          status: "ACTIVE",
          unreadCount: 0,
          updatedAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
          messages: [
            { id: "5", senderType: "LEAD", content: "Hi, can you update our subscription limits please?", createdAt: new Date(Date.now() - 120 * 60 * 1000).toISOString() },
            { id: "6", senderType: "OPERATOR", content: "I've updated your subscription limits now.", createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString() }
          ]
        }
      ];
    } finally {
      isLoading = false;
    }
  }

  onMount(() => {
    fetchConversations();
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  });

  // Selected conversation computed helper
  $: selectedChat = conversations.find(c => c.id === selectedId) || null;

  // Filter and search logic
  $: filteredConversations = conversations.filter(c => {
    const query = searchQuery.toLowerCase();
    const matchesSearch = c.leadUsername.toLowerCase().includes(query) ||
                          c.aiState.toLowerCase().includes(query) ||
                          (c.messages && c.messages.some(m => m.content.toLowerCase().includes(query)));

    if (!matchesSearch) return false;

    if (activeFilter === 'Active') return c.status === 'ACTIVE';
    if (activeFilter === 'Waiting') return c.status === 'WAITING';
    return true; // 'All'
  });

  // Grouped conversations for visual sectioning (matches mockup)
  $: waitingConversations = filteredConversations.filter(c => c.status === 'WAITING');
  $: activeConversations = filteredConversations.filter(c => c.status === 'ACTIVE');

  // Select a conversation
  function selectConversation(id) {
    selectedId = id;
    if (isMobileView) {
      showMobileChat = true;
    }
  }

  // Handle back button on mobile
  function handleBackToList() {
    showMobileChat = false;
  }

  // Send manual reply
  async function sendReply() {
    if (!replyText.trim() || !selectedId) return;

    const currentId = selectedId;
    const body = { content: replyText };
    replyText = ''; // Clear input immediately for high responsiveness (Neurophilosophical instant response)

    try {
      const res = await fetch(`/api/conversations/${currentId}/reply`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      if (res.ok) {
        const newMsg = await res.json();

        // Optimistic UI update or full fetch
        conversations = conversations.map(c => {
          if (c.id === currentId) {
            return {
              ...c,
              status: 'ACTIVE',
              unreadCount: 0,
              updatedAt: new Date().toISOString(),
              messages: [...c.messages, newMsg]
            };
          }
          return c;
        });
      } else {
        alert('Failed to send reply');
      }
    } catch (err) {
      console.error('Failed to post reply', err);
      // Fallback local update if offline
      const mockReply = {
        id: Math.random().toString(),
        senderType: "OPERATOR",
        content: body.content,
        createdAt: new Date().toISOString()
      };
      conversations = conversations.map(c => {
        if (c.id === currentId) {
          return {
            ...c,
            status: 'ACTIVE',
            unreadCount: 0,
            updatedAt: new Date().toISOString(),
            messages: [...c.messages, mockReply]
          };
        }
        return c;
      });
    }
  }

  // Helper to format timestamps to human-readable wait time (e.g., "2m ago")
  function formatTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    const diffMs = Date.now() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHrs = Math.floor(diffMin / 60);
    const diffDays = Math.floor(diffHrs / 24);

    if (diffSec < 60) return 'Just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffHrs < 24) return `${diffHrs}h ago`;
    return `${diffDays}d ago`;
  }

  // Keyboard accessibility
  function handleKeyPress(e, id) {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      selectConversation(id);
    }
  }
</script>

<div class="min-h-screen bg-surface text-on-surface flex flex-col">
  <!-- TopAppBar Header -->
  <header class="fixed top-0 left-0 w-full z-50 flex justify-between items-center px-4 h-14 bg-surface border-b border-outline-variant">
    <div class="flex items-center gap-3">
      <div class="w-8 h-8 rounded-full overflow-hidden border border-outline-variant">
        <img class="w-full h-full object-cover" alt="Operator avatar" src="https://lh3.googleusercontent.com/aida-public/AB6AXuC8JX17bK7CgeNn6crr_45ocB1uMEQkhTOqvLD3OZ1mcGzPqllElt7RhCGAbSaJ7i9psfUfJlWT7EJJhTaGVE9bEKG4T0ycUhmICHMy_ndmczGNegfgGdLAQwyHDWw6J-h_zkgXWDTqJCY5pTcxcnczEugb2mVGzA4fXVup0pSFMjCplPEADD4YGePbyCFp_6_sz65DV1j87Pwpa0eu5zrJFu2XgeqgJsKwz-2YYxzbCwTnFc0UTjVf0vG6AFCTwnM9mwytH1aaPZ0"/>
      </div>
      <h1 class="text-lg font-bold text-primary">Conversations</h1>
    </div>
    <div class="flex gap-2">
      <button on:click={fetchConversations} aria-label="Refresh conversations" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container-highest text-on-surface-variant transition-colors duration-200">
        <span class="material-symbols-outlined">refresh</span>
      </button>
    </div>
  </header>

  <div class="flex-grow pt-14 flex">
    <!-- MASTER COLUMN: Left Pane (List of chats) -->
    <aside class="w-full md:w-[380px] md:border-r border-outline-variant flex flex-col bg-surface transition-all {isMobileView && showMobileChat ? 'hidden' : 'block'}">
      <!-- Search and Filters Section -->
      <section class="p-4 bg-surface sticky top-14 z-40">
        <div class="relative flex items-center mb-4">
          <span class="material-symbols-outlined absolute left-3 text-outline">search</span>
          <input
            type="text"
            placeholder="Search conversations..."
            bind:value={searchQuery}
            class="w-full pl-10 pr-4 py-2 bg-surface-container-lowest border border-outline-variant rounded-lg font-medium text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-all"
          />
        </div>

        <!-- Filter Chips -->
        <div class="flex gap-2 overflow-x-auto no-scrollbar pb-1">
          {#each ['All', 'Active', 'Waiting'] as filter}
            <button
              on:click={() => activeFilter = filter}
              class="px-4 py-1.5 rounded-full text-xs font-semibold transition-colors shrink-0 {activeFilter === filter ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'}"
            >
              {filter}
            </button>
          {/each}
        </div>
      </section>

      <!-- Skeletons Loader when fetching -->
      {#if isLoading && conversations.length === 0}
        <div class="p-4 space-y-4">
          {#each Array(3) as _}
            <div class="flex gap-3 animate-pulse">
              <div class="w-12 h-12 bg-surface-container-highest rounded-full"></div>
              <div class="flex-grow space-y-2 py-1">
                <div class="h-4 bg-surface-container-highest rounded w-1/3"></div>
                <div class="h-3 bg-surface-container-highest rounded w-2/3"></div>
              </div>
            </div>
          {/each}
        </div>
      {:else if filteredConversations.length === 0}
        <div class="p-8 text-center text-outline text-sm">
          No conversations found
        </div>
      {:else}
        <!-- Conversation List -->
        <div class="flex-grow overflow-y-auto pb-10">

          <!-- Category: Waiting for response (High Priority) -->
          {#if waitingConversations.length > 0}
            <div class="px-4 py-2">
              <h2 class="text-[10px] font-bold text-outline uppercase tracking-wider">Waiting for Response • {waitingConversations.length}</h2>
            </div>
            {#each waitingConversations as chat (chat.id)}
              <div
                role="button"
                tabindex="0"
                on:click={() => selectConversation(chat.id)}
                on:keydown={(e) => handleKeyPress(e, chat.id)}
                class="relative bg-surface-container-lowest border-y border-outline-variant/30 px-4 py-4 flex items-start gap-3 hover:bg-surface-container-low transition-colors duration-150 cursor-pointer {selectedId === chat.id ? 'bg-surface-container' : ''}"
              >
                {#if selectedId === chat.id}
                  <div class="active-thread-indicator"></div>
                {/if}
                <div class="relative flex-shrink-0">
                  <div class="w-12 h-12 rounded-full overflow-hidden border border-outline-variant bg-surface-container-high flex items-center justify-center font-bold text-primary text-sm">
                    {chat.leadUsername ? chat.leadUsername.split(' ').map(n=>n[0]).join('') : 'U'}
                  </div>
                  <div class="absolute bottom-0 right-0 w-3 h-3 bg-secondary-fixed border-2 border-surface-container-lowest rounded-full"></div>
                </div>

                <div class="flex-grow min-w-0">
                  <div class="flex justify-between items-baseline mb-0.5">
                    <div class="flex items-center gap-1.5 min-w-0">
                      <h3 class="font-bold text-sm truncate">{chat.leadUsername}</h3>
                      {#if chat.unreadCount > 0}
                        <span class="w-2 h-2 rounded-full bg-error shrink-0" title="High Priority"></span>
                      {/if}
                    </div>
                    <span class="text-xs text-primary shrink-0">{formatTime(chat.updatedAt)}</span>
                  </div>
                  <p class="text-[10px] text-outline mb-1 truncate">{chat.aiState || 'Telegram User'}</p>
                  <p class="text-sm text-on-surface font-semibold truncate">
                    {chat.messages && chat.messages.length > 0 ? chat.messages[chat.messages.length - 1].content : 'No messages yet'}
                  </p>
                </div>

                {#if chat.unreadCount > 0}
                  <div class="flex flex-col items-end gap-2 shrink-0">
                    <div class="w-5 h-5 rounded-full bg-primary flex items-center justify-center">
                      <span class="text-[10px] font-bold text-on-primary">{chat.unreadCount}</span>
                    </div>
                  </div>
                {/if}
              </div>
            {/each}
          {/if}

          <!-- Category: Active Threads -->
          {#if activeConversations.length > 0}
            <div class="px-4 py-2 mt-4">
              <h2 class="text-[10px] font-bold text-outline uppercase tracking-wider">Active Threads • {activeConversations.length}</h2>
            </div>
            {#each activeConversations as chat (chat.id)}
              <div
                role="button"
                tabindex="0"
                on:click={() => selectConversation(chat.id)}
                on:keydown={(e) => handleKeyPress(e, chat.id)}
                class="relative bg-surface-container-lowest border-y border-outline-variant/30 px-4 py-4 flex items-start gap-3 hover:bg-surface-container-low transition-colors duration-150 cursor-pointer {selectedId === chat.id ? 'bg-surface-container' : ''}"
              >
                {#if selectedId === chat.id}
                  <div class="active-thread-indicator"></div>
                {/if}
                <div class="relative flex-shrink-0">
                  <div class="w-12 h-12 rounded-full overflow-hidden border border-outline-variant bg-surface-container-high flex items-center justify-center font-bold text-outline text-sm">
                    {chat.leadUsername ? chat.leadUsername.split(' ').map(n=>n[0]).join('') : 'U'}
                  </div>
                  <div class="absolute bottom-0 right-0 w-3 h-3 bg-secondary-fixed border-2 border-surface-container-lowest rounded-full"></div>
                </div>

                <div class="flex-grow min-w-0">
                  <div class="flex justify-between items-baseline mb-0.5">
                    <h3 class="font-bold text-sm truncate">{chat.leadUsername}</h3>
                    <span class="text-xs text-outline shrink-0">{formatTime(chat.updatedAt)}</span>
                  </div>
                  <p class="text-[10px] text-outline mb-1 truncate">{chat.aiState || 'Telegram User'}</p>
                  <p class="text-sm text-on-surface-variant truncate">
                    {#if chat.messages && chat.messages.length > 0}
                      {#if chat.messages[chat.messages.length - 1].senderType === 'OPERATOR'}
                        <span class="font-semibold text-primary">You: </span>
                      {/if}
                      {chat.messages[chat.messages.length - 1].content}
                    {:else}
                      No messages yet
                    {/if}
                  </p>
                </div>
              </div>
            {/each}
          {/if}

        </div>
      {/if}
    </aside>

    <!-- DETAIL COLUMN: Right Pane (Chat conversation details) -->
    <main class="flex-grow flex flex-col bg-surface-container-low transition-all {isMobileView && !showMobileChat ? 'hidden' : 'flex'}">
      {#if selectedChat}
        <!-- Detail Header -->
        <header class="h-14 px-4 border-b border-outline-variant bg-surface-container-lowest flex items-center justify-between z-10">
          <div class="flex items-center gap-3">
            {#if isMobileView}
              <button on:click={handleBackToList} aria-label="Go back to list" class="w-10 h-10 flex items-center justify-center rounded-full hover:bg-surface-container-high text-primary shrink-0 transition-all">
                <span class="material-symbols-outlined font-bold text-lg">arrow_back</span>
              </button>
            {/if}
            <div>
              <div class="flex items-center gap-2">
                <h2 class="font-bold text-sm md:text-base text-on-surface">{selectedChat.leadUsername}</h2>
                <span class="text-xs px-2.5 py-0.5 rounded-full font-bold {selectedChat.status === 'WAITING' ? 'bg-error-container text-on-error-container' : 'bg-secondary-container text-on-secondary-container'}">
                  {selectedChat.status}
                </span>
              </div>
              <p class="text-xs text-outline">{selectedChat.aiState || 'Active dialogue'}</p>
            </div>
          </div>
          <div class="text-right text-xs text-outline hidden sm:block">
            {selectedChat.leadPhone}
          </div>
        </header>

        <!-- Messages Area -->
        <div class="flex-grow overflow-y-auto p-4 space-y-4 bg-surface-container-low flex flex-col justify-end min-h-[400px]">
          <div class="space-y-4">
            {#each selectedChat.messages as msg (msg.id)}
              <div class="flex flex-col {msg.senderType === 'OPERATOR' ? 'items-end' : 'items-start'}">
                <div class="max-w-[75%] rounded-xl px-4 py-2.5 text-sm shadow-sm transition-all
                  {msg.senderType === 'OPERATOR' ? 'bg-primary text-on-primary rounded-tr-none' : 'bg-surface-container-lowest text-on-surface border border-outline-variant/30 rounded-tl-none'}
                ">
                  <!-- AI or Lead header -->
                  {#if msg.senderType === 'AI'}
                    <div class="text-[10px] font-bold text-tertiary uppercase tracking-wide mb-1 flex items-center gap-1">
                      <span class="material-symbols-outlined text-xs">smart_toy</span>
                      AI Dialogue Agent
                    </div>
                  {:else if msg.senderType === 'LEAD'}
                    <div class="text-[10px] font-bold text-outline uppercase tracking-wide mb-1">
                      Lead
                    </div>
                  {/if}
                  <p class="leading-relaxed whitespace-pre-wrap">{msg.content}</p>
                </div>
                <span class="text-[9px] text-outline mt-1 px-1">{formatTime(msg.createdAt)}</span>
              </div>
            {/each}
          </div>
        </div>

        <!-- Operator Action Reply Form -->
        <footer class="p-4 bg-surface-container-lowest border-t border-outline-variant">
          <form on:submit|preventDefault={sendReply} class="flex items-center gap-2">
            <input
              type="text"
              placeholder="Type your manual reply here..."
              bind:value={replyText}
              class="flex-grow px-4 py-3 bg-surface border border-outline-variant rounded-xl font-medium text-sm focus:outline-none focus:ring-2 focus:ring-primary focus:border-primary transition-all"
              aria-label="Manual reply message content"
            />
            <button
              type="submit"
              disabled={!replyText.trim()}
              class="h-11 px-6 rounded-xl bg-primary text-on-primary font-bold text-sm hover:bg-primary/95 disabled:bg-surface-container-high disabled:text-outline transition-all flex items-center justify-center gap-1 shrink-0"
            >
              <span class="material-symbols-outlined text-base">send</span>
              <span>Send</span>
            </button>
          </form>
        </footer>
      {:else}
        <!-- Empty Chat details placeholder -->
        <div class="flex-grow flex flex-col items-center justify-center text-center p-8 bg-surface-container-low text-outline">
          <span class="material-symbols-outlined text-5xl mb-4 text-outline/60" style="font-variation-settings: 'FILL' 1;">chat</span>
          <h2 class="font-bold text-lg text-on-surface-variant mb-1">No thread selected</h2>
          <p class="text-sm max-w-xs">Select any cold contact thread from the sidebar to view metrics, dialogue history, and send manual takeovers.</p>
        </div>
      {/if}
    </main>
  </div>
</div>
