/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#12151a",
        panel: "#1a1e26",
        line: "#2a2f3a",
        bifrost: "#c9a13b",
        merge: "#4fae7a",
        review: "#d6a24a",
        block: "#c9564b"
      },
      fontFamily: {
        display: ["'IBM Plex Serif'", "serif"],
        mono: ["'IBM Plex Mono'", "monospace"]
      }
    }
  },
  plugins: []
};
