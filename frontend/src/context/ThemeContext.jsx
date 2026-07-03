// Stub — app uses fixed dark theme
export function ThemeProvider({ children }) { return children; }
export function useTheme() { return { dark: true, toggleTheme: () => {} }; }
