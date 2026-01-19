import Link from "next/link";
import {
  ClerkProvider,
  SignedIn,
  SignedOut,
  UserButton,
  OrganizationSwitcher,
} from "@clerk/nextjs";
import "./globals.css";

export const metadata = {
  title: "RFP Document Manager",
  description: "Upload and analyze RFP documents",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <ClerkProvider>
      <html lang="en">
        <body className="min-h-screen font-sans">
          <nav className="px-6 py-4 border-b border-gray-200 flex items-center bg-white">
            <Link href="/" className="text-lg font-semibold text-gray-900 hover:text-gray-700">
              RFP Manager
            </Link>

            <SignedIn>
              <div className="flex items-center gap-6 ml-8">
                <Link href="/" className="text-gray-600 hover:text-gray-900 transition-colors">
                  Dashboard
                </Link>
                <Link href="/documents" className="text-gray-600 hover:text-gray-900 transition-colors">
                  Documents
                </Link>
              </div>

              <div className="ml-auto flex items-center gap-4">
                <OrganizationSwitcher
                  hidePersonal
                  afterSelectOrganizationUrl="/"
                  appearance={{
                    elements: {
                      rootBox: "flex items-center",
                    },
                  }}
                />
                <UserButton afterSignOutUrl="/sign-in" />
              </div>
            </SignedIn>

            <SignedOut>
              <div className="ml-auto flex items-center gap-4">
                <Link
                  href="/sign-in"
                  className="text-gray-600 hover:text-gray-900 transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  href="/sign-up"
                  className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                >
                  Sign Up
                </Link>
              </div>
            </SignedOut>
          </nav>
          <main className="p-6">{children}</main>
        </body>
      </html>
    </ClerkProvider>
  );
}
