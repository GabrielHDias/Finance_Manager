import AppNavigation from "@/components/app-navigation";

export default function ProtectedLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <>
      <AppNavigation />

      {children}
    </>
  );
}