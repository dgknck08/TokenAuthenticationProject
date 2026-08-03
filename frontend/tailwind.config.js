/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}", // frontend/src klasöründeki dosyaları tarar
  ],
  theme: {
    extend: {
      fontFamily: {
        // styles.css'teki --font-display / --font-sans değişkenleriyle aynı yığınlar
        display: ['Fraunces', 'Georgia', 'serif'],
        sans: ['Inter', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
