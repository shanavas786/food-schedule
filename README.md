# 🧹 Usthad Chelav — Jetpack Compose



---

## Tech Stack
| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + StateFlow |
| Persistence | SharedPreferences + Gson |
| Reordering | `compose-reorderable` (burnoutcrew) |
| Language | Kotlin |

---

## Features

| Feature         | How |
|-----------------|-----|
| View Members    | Scrollable list with name, phone, and date badge on the right |
| Add member      | Blue **+** FAB → dialog with name, phone, WhatsApp fields |
| Edit memeber    | ✏ icon on card |
| Delete member   | 🗑 icon → confirmation dialog |
| Skip iteration  | 🚫 icon grays out employee; they're skipped in rotation |
| Drag to reorder | Long-press the **≡** handle and drag |
| Assign next day | Tap **✔ Assign Next Day** in the header |
| Call            | 📞 icon dials number via system dialer |
| WhatsApp        | 💬 icon opens WhatsApp conversation |
| Persistence     | All data saved locally, survives app restarts |

---

