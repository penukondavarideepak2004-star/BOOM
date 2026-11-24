"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { Menu, X } from "lucide-react";
import { motion, AnimatePresence, useReducedMotion } from "framer-motion";
import { siteConfig } from "@/data/portfolio";
import { cn } from "@/lib/utils";

const NAV_LINKS = [
  { label: "About", href: "#about" },
  { label: "Skills", href: "#skills" },
  { label: "Projects", href: "#projects" },
  { label: "Contact", href: "#contact" },
];

export default function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const prefersReducedMotion = useReducedMotion();

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Close mobile menu on resize to desktop
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth >= 768) setMobileOpen(false);
    };
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  // Prevent body scroll when mobile menu is open
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? "hidden" : "";
    return () => { document.body.style.overflow = ""; };
  }, [mobileOpen]);

  const handleNavClick = (href: string) => {
    setMobileOpen(false);
    // Let the browser handle smooth scroll via CSS
    const el = document.querySelector(href);
    el?.scrollIntoView({ behavior: prefersReducedMotion ? "instant" : "smooth" });
  };

  const menuVariants = {
    hidden: { opacity: 0, y: -16 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.25, ease: "easeOut" } },
    exit: { opacity: 0, y: -16, transition: { duration: 0.2 } },
  };

  return (
    <header
      className={cn(
        "fixed top-0 left-0 right-0 z-50 transition-all duration-300",
        scrolled
          ? "bg-[#101110]/90 backdrop-blur-md border-b border-[rgba(166,173,164,0.1)] shadow-lg"
          : "bg-transparent"
      )}
    >
      <nav
        className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between"
        aria-label="Main navigation"
      >
        {/* Logo */}
        <Link
          href="#hero"
          onClick={(e) => {
            e.preventDefault();
            handleNavClick("#hero");
          }}
          className="text-[#F4F4F0] font-bold text-xl tracking-tight hover:text-[#C6FF4D] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] rounded"
          aria-label="Back to top"
        >
          <span className="text-[#C6FF4D]">{siteConfig.initials}</span>
          <span className="hidden sm:inline text-[#F4F4F0]">
            {" "}
            {siteConfig.name.split(" ")[0]}
          </span>
        </Link>

        {/* Desktop nav */}
        <ul className="hidden md:flex items-center gap-6" role="list">
          {NAV_LINKS.map((link) => (
            <li key={link.href}>
              <button
                onClick={() => handleNavClick(link.href)}
                className="text-[#A7ADA4] hover:text-[#F4F4F0] text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] rounded px-1"
              >
                {link.label}
              </button>
            </li>
          ))}
        </ul>

        {/* Desktop CTA */}
        <div className="hidden md:block">
          <button
            onClick={() => handleNavClick("#contact")}
            className="px-4 py-2 rounded-lg bg-[#C6FF4D] text-[#101110] text-sm font-semibold hover:bg-[#d4ff70] active:scale-95 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] focus-visible:ring-offset-2 focus-visible:ring-offset-[#101110]"
          >
            Contact Me
          </button>
        </div>

        {/* Mobile hamburger */}
        <button
          onClick={() => setMobileOpen((v) => !v)}
          className="md:hidden p-2 rounded-lg text-[#A7ADA4] hover:text-[#F4F4F0] hover:bg-[#1B1E1B] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D]"
          aria-label={mobileOpen ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={mobileOpen}
          aria-controls="mobile-menu"
        >
          {mobileOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {/* Mobile menu */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            id="mobile-menu"
            variants={prefersReducedMotion ? {} : menuVariants}
            initial="hidden"
            animate="visible"
            exit="exit"
            className="md:hidden bg-[#101110]/95 backdrop-blur-md border-b border-[rgba(166,173,164,0.1)]"
          >
            <ul
              className="max-w-6xl mx-auto px-4 py-6 flex flex-col gap-2"
              role="list"
            >
              {NAV_LINKS.map((link) => (
                <li key={link.href}>
                  <button
                    onClick={() => handleNavClick(link.href)}
                    className="w-full text-left px-4 py-3 rounded-lg text-[#F4F4F0] font-medium hover:bg-[#1B1E1B] hover:text-[#C6FF4D] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D]"
                  >
                    {link.label}
                  </button>
                </li>
              ))}
              <li className="mt-2">
                <button
                  onClick={() => handleNavClick("#contact")}
                  className="w-full px-4 py-3 rounded-lg bg-[#C6FF4D] text-[#101110] font-semibold hover:bg-[#d4ff70] active:scale-[0.98] transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D]"
                >
                  Contact Me
                </button>
              </li>
            </ul>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
