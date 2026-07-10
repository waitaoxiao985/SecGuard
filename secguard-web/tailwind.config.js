/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {
      colors: {
        'sg-primary': '#4f46e5',
        'sg-dark': '#0f172a'
      }
    }
  },
  plugins: []
}
