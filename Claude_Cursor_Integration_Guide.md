# Hướng Dẫn Toàn Diện: Claude Code + Cursor IDE Integration

**Ngày:** 2026-06-11 | **Phiên bản:** 1.0

---

## Mục Lục

1. [Tổng Quan Kiến Trúc](#1-tổng-quan-kiến-trúc)
2. [MCP Setup](#2-mcp-setup)
3. [Global Config Files](#3-global-config-files)
4. [Task Handoff Protocol](#4-task-handoff-protocol)
5. [Workflow Thực Tế](#5-workflow-thực-tế)
6. [Prompt Templates Cho Cursor](#6-prompt-templates-cho-cursor)
7. [File Structure Tổng Hợp](#7-file-structure-tổng-hợp)
8. [Known Limitations](#8-known-limitations)
9. [Troubleshooting](#9-troubleshooting)

---

## 1. Tổng Quan Kiến Trúc

### 1.1 Triết Lý Thiết Kế

Hệ thống tách biệt hai vai trò rõ ràng:

| Tool | Vai Trò | Trách Nhiệm |
|------|---------|-------------|
| **Claude Code CLI** (terminal) | PM / Product Owner | Lên plan, break down tasks, review kết quả, quyết định kiến trúc |
| **Cursor IDE** (editor) | Developer | Đọc task specs, implement code, chạy build/test, báo blocked |

Hai session **hoàn toàn độc lập**. Giao tiếp duy nhất qua **file trên disk**.

### 1.2 Sơ Đồ Kiến Trúc Tổng Thể

```
┌──────────────────────┐         ┌──────────────────────────────┐
│   CLAUDE CODE CLI    │         │       CURSOR IDE             │
│   Role: PM / PO      │         │       Role: Developer        │
│                      │         │                              │
│  - Lên plan          │         │  - Đọc task specs            │
│  - Break down tasks  │         │  - Implement code            │
│  - Review kết quả    │         │  - Chạy build/test           │
│  - Quyết định KT     │         │  - Báo [!] blocked           │
└──────────┬───────────┘         └────────────┬─────────────────┘
           │     GIAO TIẾP QUA FILE SYSTEM    │
           ▼                                  ▼
┌───────────────────────────────────────────────────────────────┐
│  .claude/tasks.md          ← handoff chính                   │
│  .claude/session-sync.json ← trạng thái CLI session          │
│  PLAN.md / CURSOR_PLAN.md  ← sprint plan                     │
│  .claude/memory/MEMORY.md  ← project memory                  │
└───────────────────────────────────────────────────────────────┘
```

### 1.3 Luồng Giao Tiếp

```
CLAUDE CODE CLI                    CURSOR IDE
───────────────                    ──────────
1. Nhận yêu cầu từ user
2. Phân tích vấn đề
3. Viết .claude/tasks.md
   với các task [ ]
          │
          │ (write to disk)
          ▼
    [tasks.md updated]
          │
          │ (read from disk)
          ▼
                              4. Đọc tasks.md lúc startup
                              5. Tìm task [ ] đầu tiên
                              6. Đổi [~] → implement
                              7. Chạy Verify command
                              8. Đổi [x] hoặc [!]
                                     │
                                     │ (write to disk)
                                     ▼
                               [tasks.md updated]
                                     │
                                     │ (read from disk)
                                     ▼
9. Đọc [x] done tasks
10. Review kết quả
11. Viết tasks tiếp theo
```

---

## 2. MCP Setup

**File cấu hình:** `~/.cursor/mcp.json`

```json
{
  "mcpServers": {
    "claude-code": {
      "command": "/Users/leruyn/.nvm/versions/node/v20.19.5/bin/claude",
      "args": ["mcp", "serve"]
    },
    "ask-claude": {
      "command": "python3",
      "args": ["/Users/leruyn/.claude/mcp/ask_claude_server.py"]
    },
    "codegraph": {
      "command": "/Users/leruyn/.nvm/versions/node/v20.19.5/bin/codegraph",
      "args": ["serve", "--mcp"],
      "env": { "NODE_OPTIONS": "--max-old-space-size=4096" }
    }
  }
}
```

### 2.1 Server: `claude-code`

Lệnh khởi động: `claude mcp serve` — expose **30 tools**:

| Nhóm | Tools |
|------|-------|
| File | `Read`, `Write`, `Edit`, `MultiEdit` |
| Shell | `Bash` |
| Search | `Grep`, `Glob`, `WebSearch`, `WebFetch` |
| Tasks | `TaskCreate`, `TaskList`, `TaskUpdate`, `TaskGet`, `TaskStop` |
| Scheduling | `CronCreate`, `CronList`, `CronDelete` |

> ⚠️ **KHÔNG dùng `Agent` tool từ Cursor** → lỗi *"Agent type not found"*
> Subagent types không available trong Cursor context. Thay bằng `ask_claude`.

### 2.2 Server: `ask-claude` (Custom)

**Script:** `~/.claude/mcp/ask_claude_server.py`

Dùng **claude.ai account** của user — không cần Anthropic API key riêng.

```
Cursor gọi ask_claude(prompt, context_file?, context_text?)
        │
        ▼
ask_claude_server.py
        │  subprocess.run("claude -p '...' --output-format text")
        ▼
claude CLI  ←  dùng claude.ai account tokens
        │
        ▼
Response trả về Cursor chat
```

**Tool signature:**

```
ask_claude(
    prompt: str,              # BẮT BUỘC
    context_file: str = None, # path đến file context
    context_text: str = None  # text inline làm context
)
```

> ⚠️ **Stateless** — mỗi lần gọi là session Claude mới, không có memory.
> Luôn truyền đầy đủ context qua `context_file` hoặc `context_text`.

---

## 3. Global Config Files

### 3.1 `~/.claude/CLAUDE.md` — Claude Code Global Role

Định nghĩa vai trò PM/PO cho Claude Code CLI trên mọi project.

**Nguyên tắc viết task:**

| Trường | Yêu cầu |
|--------|---------|
| `Action` | Copy-pasteable, không có "consider" hay "maybe" |
| `Verify` | Shell command cụ thể, không phải "looks good" |
| Scope | 1 task = 1 atomic change (2 files = 2 tasks) |
| Thứ tự | Dependency order, Cursor chạy top-to-bottom |

### 3.2 `~/.cursor/rules/claude-code-bridge.md`

**Session startup** — Cursor tự động đọc (không báo user):

1. `.claude/session-sync.json` → Claude Code đang làm gì?
2. `PLAN.md` / `CURSOR_PLAN.md` → Sprint hiện tại?
3. `.claude/memory/MEMORY.md` → Project memory
4. `CLAUDE.md` → Project-specific instructions

**Tool usage:**
- ✅ `claude-code.Bash`, `Read`, `Edit`, `Write`, `TaskCreate`, `WebSearch`
- ✅ `ask-claude.ask_claude`
- ❌ `claude-code.Agent` → ERROR

### 3.3 `~/.cursor/rules/action-confirmation.md`

**Không cần confirm (safe):**
- Read/search/grep, build, test, lint, tạo file mới, edit file

**Bắt buộc confirm (destructive):**
- Xóa file, `git push/reset --hard/clean`, install packages, deploy, sửa global config, lệnh có `--force/DROP TABLE/DELETE FROM`

**Confirm nếu scope lớn:**
- Touching >5 files, rename public API, xóa >1 file

**Sau khi `ask_claude` trả plan:**
1. Hiển thị full plan
2. **DỪNG** — không execute gì cả
3. Hỏi: *"Áp dụng plan này không?"*
4. Chỉ proceed sau khi user confirm

> Cách hỏi đúng: *"About to delete KeyValueStorage.kt and remove 4 references in 2 files. Proceed?"*
> Không hỏi chung: *"Should I continue?"*

### 3.4 `~/.cursor/rules/core-rules.md`

- Preserve existing architecture
- Additive > rewrite, minimal changes
- Before implementation: summarize → list files → giải thích tại sao
- Never rewrite unrelated code, rename modules, change architecture without approval

---

## 4. Task Handoff Protocol

### Status Markers

```
[ ] pending   — Claude viết, Cursor chưa bắt đầu
[~] working   — Cursor đang implement
[x] done      — Hoàn thành + Verify passed
[!] blocked   — Cần Claude quyết định
```

### Task Format Chuẩn

```markdown
## TASK-NNN [ ] <tên ngắn gọn>
**File:** path/to/file.kt:line
**Action:** thay đổi cụ thể — không mơ hồ
**Verify:** lệnh shell để chứng minh đã xong
**Depends:** TASK-NNN (bỏ nếu không có)
```

**Ví dụ:**

```markdown
## TASK-001 [ ] Thêm Firestore dependency
**File:** shared/build.gradle.kts:45
**Action:** Thêm `implementation("dev.gitlive:firebase-firestore:1.12.0")` vào block dependencies
**Verify:** grep -n "firebase-firestore" shared/build.gradle.kts
**Depends:** (none)

## TASK-002 [!] Quyết định offline cache strategy
**File:** (chưa xác định)
**Action:** BLOCKED — SQLDelight hay in-memory cache?
**Result:** Không rõ pattern nào phù hợp với KMP iOS + Android
```

### Execution Rules Cho Cursor

```
Tìm task [ ] đầu tiên (kiểm tra Depends:)
        │
        ▼
Đổi [~] TRƯỚC KHI touch bất kỳ file nào
        │
        ▼
Implement đúng Action — không thêm, không bớt
        │
        ▼
Chạy Verify
        │
        ├── PASS → [x] + **Result:** <one line> → task tiếp theo
        └── FAIL → [!] + ghi lý do → task độc lập khác
```

---

## 5. Workflow Thực Tế

### Task đơn giản → Prompt thẳng Cursor

```
User → Cursor: "Fix typo 'recieve' thành 'receive' trong LoginScreen.kt"
Cursor: Đọc → Edit → Done
```

### Task phức tạp → Claude Code → tasks.md → Cursor

```
[Claude Code terminal]
User → Claude Code: "Lên plan fix 9 compile errors"
Claude Code: Phân tích → viết TASK-001..009 vào tasks.md

[Cursor IDE]
User → Cursor: "Đọc .claude/tasks.md, thực hiện task [ ] theo thứ tự"
Cursor: Execute → update status → báo [!] nếu blocked

[Nếu có [!]]
User → Claude Code: "Xem tasks.md, có TASK-005 blocked"
Claude Code: Quyết định → viết lại task → Cursor tiếp tục
```

### Cần Claude reasoning trong Cursor

```
User → Cursor: "Dùng ask_claude với context_file .claude/tasks.md:
                Nên dùng expect/actual hay wrapper interface?"
        │
ask_claude → claude CLI → response
        │
Cursor: Hiển thị plan → "Áp dụng plan này không?"
User: "Có" → Cursor implement
```

---

## 6. Prompt Templates Cho Cursor

```
# Execute tasks
"Đọc .claude/tasks.md và thực hiện các task [ ] theo thứ tự"

# Gọi Claude với context file
"Dùng ask_claude với context_file là .claude/tasks.md: [câu hỏi]"

# Gọi Claude với inline context
"Dùng ask_claude với context_text là '[nội dung]': [câu hỏi]"

# Review trước commit
"Dùng ask_claude với context_file là .claude/tasks.md:
 Tất cả tasks [x] done. Review implementation có đúng plan không?"

# Architecture decision
"Dùng ask_claude với context_text là:
 'Project: KMP (Android + iOS), Problem: [mô tả]'
 Nên implement theo hướng nào? Cho plan với file paths."

# Cross-session handoff
"Đọc .claude/session-sync.json và .claude/tasks.md.
 Claude Code vừa làm gì? Tôi cần tiếp tục từ đâu?"
```

---

## 7. File Structure Tổng Hợp

```
~/.claude/
├── CLAUDE.md                    ← Global Claude Code role (PM/PO)
└── mcp/
    └── ask_claude_server.py     ← Custom MCP server

~/.cursor/
├── mcp.json                     ← MCP server registrations
└── rules/
    ├── claude-code-bridge.md    ← Session startup + tool usage
    ├── action-confirmation.md   ← Confirmation policy
    └── core-rules.md            ← Core behavior rules

<project>/.claude/
├── tasks.md                     ← Task handoff (main)
└── session-sync.json            ← Claude Code session state
```

| File | Ai Viết | Ai Đọc |
|------|---------|--------|
| `~/.claude/CLAUDE.md` | Bạn (1 lần) | Claude Code |
| `ask_claude_server.py` | Bạn (1 lần) | Python runtime |
| `~/.cursor/mcp.json` | Bạn (1 lần) | Cursor |
| `rules/*.md` | Bạn (1 lần) | Cursor |
| `tasks.md` | Claude Code | Cursor update status |
| `session-sync.json` | Claude Code (auto) | Cursor (startup) |

---

## 8. Known Limitations

| Vấn đề | Nguyên nhân | Workaround |
|--------|-------------|------------|
| `Agent` tool lỗi từ Cursor | Subagent types không available trong MCP context | Dùng `ask_claude` |
| `ask_claude` stateless | Mỗi lần = `claude -p` process mới | Luôn truyền context_file/context_text |
| `claude.ai` ≠ Anthropic API key | Hai sản phẩm khác nhau | claude.ai cho CLI, API key cho Cursor model provider |
| Hai session không tự sync | Hai process độc lập | Đọc `tasks.md` + `session-sync.json` thủ công |

---

## 9. Troubleshooting

### "Agent type not found"
Không dùng `claude-code.Agent` từ Cursor. Thay bằng `ask_claude`.

### `ask_claude` timeout

```bash
# Test binary trực tiếp
/Users/leruyn/.nvm/versions/node/v20.19.5/bin/claude -p "hello" --output-format text

# Nếu lỗi auth
claude login

# Nếu path sai (sau update Node)
which claude  # copy đường dẫn mới vào CLAUDE_BIN trong script
```

### Cursor không load rules
Cmd+Shift+P → "Reload Window". Kiểm tra `ls ~/.cursor/rules/`.

### MCP server không start

```bash
# Test ask_claude server
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}' | \
  python3 ~/.claude/mcp/ask_claude_server.py
# Phải trả về JSON response
```

### tasks.md out of sync
Đọc `session-sync.json` trước khi bắt đầu. Không để 2 session chạy song song trên cùng feature.

---

## Quick Reference

**Status markers:** `[ ]` pending · `[~]` working · `[x]` done · `[!]` blocked

| Việc cần làm | Dùng gì | Ở đâu |
|---|---|---|
| Lên plan, break down tasks | Claude Code CLI | Terminal |
| Review architecture | Claude Code CLI | Terminal |
| Implement code | Cursor | IDE |
| Chạy build/test | Cursor (Bash MCP) | IDE |
| Hỏi Claude trong Cursor | `ask_claude` tool | IDE |
| Resolve blocked tasks | Claude Code CLI | Terminal |
| Simple edits (1-2 files) | Cursor native | IDE |
| Cross-session handoff | `.claude/tasks.md` | File |

---

*Cập nhật lần cuối: 2026-06-11 · Setup tại `~/.cursor/mcp.json`, `~/.claude/CLAUDE.md`, `~/.cursor/rules/`*
