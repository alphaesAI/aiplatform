function AuthLayout({ children }: LayoutProps<"/auth">) {
  return (
    <main className="flex flex-col items-center justify-center w-full min-h-screen p-4">
      {children}
    </main>
  );
}

export default AuthLayout;
