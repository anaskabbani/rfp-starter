# Next.js & React Learning Guide

## What You Currently Have

Your project uses:
- **Next.js 15.0.3** (latest version)
- **React 18.3.1** (latest stable)
- **TypeScript** (type-safe JavaScript)
- **App Router** (Next.js 13+ file-based routing)
- **Client Components** (components that run in the browser)

## Core React Concepts You're Already Using

### 1. **Components** (`page.tsx`, `layout.tsx`)
Components are reusable pieces of UI. You have:
- `Home` component in `page.tsx`
- `RootLayout` component in `layout.tsx`

**Key Concept:** Components are functions that return JSX (HTML-like syntax).

### 2. **Hooks** (`useState`, `useEffect`)
You're using two essential React hooks:

**`useState`** - Manages component state (data that can change):
```typescript
const [tenant, setTenant] = useState<string>("(loading)");
// tenant = current value
// setTenant = function to update the value
```

**`useEffect`** - Runs side effects (like API calls):
```typescript
useEffect(() => {
  // This runs after the component renders
  axios.get(...)
}, []); // Empty array = runs once on mount
```

### 3. **JSX** (JavaScript XML)
The HTML-like syntax you return:
```typescript
return (
  <div>
    <h1>Welcome 👋</h1>
  </div>
);
```

### 4. **Props** (in `layout.tsx`)
Data passed to components:
```typescript
function RootLayout({ children }: { children: React.ReactNode }) {
  // children is a prop
}
```

## Next.js Concepts You're Using

### 1. **App Router** (File-based Routing)
- `app/page.tsx` → `/` route
- `app/layout.tsx` → Wraps all pages
- `app/about/page.tsx` → `/about` route (if you create it)

### 2. **Client vs Server Components**
- `"use client"` directive = runs in browser (can use hooks, state)
- No directive = Server Component (runs on server, better for SEO)

### 3. **Environment Variables**
- `process.env.NEXT_PUBLIC_API_BASE` - accessible in browser
- Variables prefixed with `NEXT_PUBLIC_` are exposed to client

## Learning Path

### Phase 1: React Fundamentals (Week 1-2)

**1. Components & JSX**
- [ ] Create a simple `<Button>` component
- [ ] Create a `<Card>` component
- [ ] Practice composing components

**2. Props**
- [ ] Pass data from parent to child
- [ ] Use TypeScript types for props
- [ ] Practice prop drilling

**3. State Management**
- [ ] Use `useState` for form inputs
- [ ] Handle multiple state variables
- [ ] Understand when to use state vs props

**4. Event Handling**
- [ ] Handle button clicks
- [ ] Handle form submissions
- [ ] Prevent default behavior

**Practice Project:** Build a Todo List app

### Phase 2: React Hooks Deep Dive (Week 3)

**1. useEffect**
- [ ] Fetch data on component mount
- [ ] Clean up subscriptions
- [ ] Understand dependency array

**2. Other Hooks**
- [ ] `useRef` - Access DOM elements
- [ ] `useContext` - Share data across components
- [ ] `useMemo` - Optimize expensive calculations
- [ ] `useCallback` - Optimize function references

**Practice Project:** Build a Weather App with API calls

### Phase 3: Next.js Features (Week 4)

**1. Routing**
- [ ] Create multiple pages
- [ ] Dynamic routes (`app/[id]/page.tsx`)
- [ ] Navigation with `next/link`
- [ ] Programmatic navigation with `useRouter`

**2. Server vs Client Components**
- [ ] Convert some client components to server components
- [ ] Fetch data in server components
- [ ] Understand when to use each

**3. API Routes**
- [ ] Create API routes in `app/api/`
- [ ] Handle GET/POST requests
- [ ] Connect to your backend

**Practice Project:** Build a Blog with multiple pages

### Phase 4: Advanced Patterns (Week 5+)

**1. Forms & Validation**
- [ ] Use Zod for form validation (you already have it!)
- [ ] Handle form state
- [ ] Show validation errors

**2. Data Fetching**
- [ ] Server-side data fetching
- [ ] Client-side data fetching
- [ ] Loading states
- [ ] Error handling

**3. State Management**
- [ ] When to use Context API
- [ ] When to use external libraries (Zustand, Redux)
- [ ] Server state vs client state

**4. Styling**
- [ ] CSS Modules
- [ ] Tailwind CSS (popular choice)
- [ ] Styled Components
- [ ] CSS-in-JS

## Practical Exercises for Your Project

### Exercise 1: Create an Org List Page
```typescript
// app/orgs/page.tsx
"use client";
import { useState, useEffect } from "react";
import axios from "axios";

export default function OrgsPage() {
  const [orgs, setOrgs] = useState([]);
  
  useEffect(() => {
    // Fetch list of orgs from your API
  }, []);
  
  return (
    <div>
      <h1>Organizations</h1>
      {/* Display list of orgs */}
    </div>
  );
}
```

### Exercise 2: Create a Form Component
```typescript
// app/components/CreateOrgForm.tsx
"use client";
import { useState } from "react";

export default function CreateOrgForm() {
  const [slug, setSlug] = useState("");
  const [name, setName] = useState("");
  
  const handleSubmit = (e) => {
    e.preventDefault();
    // POST to your API
  };
  
  return (
    <form onSubmit={handleSubmit}>
      {/* Form inputs */}
    </form>
  );
}
```

### Exercise 3: Add Navigation
```typescript
// app/components/Nav.tsx
import Link from "next/link";

export default function Nav() {
  return (
    <nav>
      <Link href="/">Home</Link>
      <Link href="/orgs">Orgs</Link>
    </nav>
  );
}
```

## Key Concepts to Master

### 1. **Component Lifecycle**
- Mount → Render → Update → Unmount
- When `useEffect` runs
- When to clean up

### 2. **Re-rendering**
- When components re-render
- How to prevent unnecessary re-renders
- React's reconciliation algorithm

### 3. **State Management**
- Local state (`useState`)
- Shared state (`useContext`, props)
- Global state (external libraries)

### 4. **TypeScript with React**
- Typing props
- Typing state
- Typing event handlers
- Using generics

## Recommended Resources

### Official Docs
- [React Docs](https://react.dev) - Start here!
- [Next.js Docs](https://nextjs.org/docs) - Framework reference

### Tutorials
- [React Tutorial](https://react.dev/learn) - Interactive tutorial
- [Next.js Learn](https://nextjs.org/learn) - Step-by-step course

### YouTube Channels
- Web Dev Simplified
- Traversy Media
- The Net Ninja

### Practice Platforms
- [React Challenges](https://github.com/alexgurr/react-coding-challenges)
- [Frontend Mentor](https://www.frontendmentor.io) - Real-world projects

## Common Beginner Mistakes to Avoid

1. **Mutating state directly**
   ```typescript
   // ❌ Wrong
   items.push(newItem);
   
   // ✅ Correct
   setItems([...items, newItem]);
   ```

2. **Missing dependency array in useEffect**
   ```typescript
   // ❌ Wrong - runs on every render
   useEffect(() => {
     fetchData();
   });
   
   // ✅ Correct
   useEffect(() => {
     fetchData();
   }, []);
   ```

3. **Not handling loading/error states**
   ```typescript
   // ✅ Good
   const [loading, setLoading] = useState(true);
   const [error, setError] = useState(null);
   ```

4. **Creating components inside other components**
   ```typescript
   // ❌ Wrong
   function Parent() {
     function Child() { ... }
     return <Child />;
   }
   ```

## Next Steps for Your Project

1. **Add more pages**
   - `/orgs` - List all organizations
   - `/orgs/[id]` - View single org
   - `/orgs/new` - Create new org

2. **Improve the UI**
   - Add a CSS framework (Tailwind CSS recommended)
   - Create reusable components
   - Add loading states
   - Add error handling

3. **Add features**
   - Form validation with Zod
   - Search/filter functionality
   - Pagination
   - Authentication UI

4. **Learn about your backend**
   - Understand the API endpoints
   - Learn about tenant context
   - Connect frontend to backend properly

## Questions to Test Your Understanding

1. What's the difference between `useState` and `useEffect`?
2. When should you use `"use client"`?
3. How do you pass data from parent to child component?
4. What happens if you forget the dependency array in `useEffect`?
5. How do you create a new route in Next.js App Router?

## Getting Help

- Stack Overflow - Search before asking
- React Discord - Community help
- Next.js GitHub Discussions
- Your project's README

---

**Remember:** Learning React/Next.js is a journey. Start simple, build projects, and gradually add complexity. You're already on the right track! 🚀

