"use client";

import { useReducedMotion } from "framer-motion";
import { motion } from "framer-motion";
import { ArrowDown } from "lucide-react";
import { siteConfig } from "@/data/portfolio";

const fadeUp = (delay = 0) => ({
  hidden: { opacity: 0, y: 24 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.6, ease: [0.25, 0.46, 0.45, 0.94], delay },
  },
});

export default function Hero() {
  const prefersReducedMotion = useReducedMotion();
  const animate = prefersReducedMotion ? "visible" : undefined;

  const handleScroll = (href: string) => {
    const el = document.querySelector(href);
    el?.scrollIntoView({
      behavior: prefersReducedMotion ? "instant" : "smooth",
    });
  };

  return (
    <section
      id="hero"
      className="section-anchor relative min-h-screen flex flex-col items-center justify-center overflow-hidden px-4 sm:px-6 lg:px-8"
      aria-label="Introduction"
    >
      {/* Decorative background blobs */}
      <div
        aria-hidden="true"
        className="absolute inset-0 pointer-events-none overflow-hidden"
      >
        <div
          className="absolute -top-40 -left-40 w-[600px] h-[600px] rounded-full opacity-[0.07]"
          style={{
            background:
              "radial-gradient(circle, #C6FF4D 0%, transparent 70%)",
          }}
        />
        <div
          className="absolute -bottom-60 right-0 w-[500px] h-[500px] rounded-full opacity-[0.05]"
          style={{
            background:
              "radial-gradient(circle, #C6FF4D 0%, transparent 70%)",
          }}
        />
        {/* Grid */}
        <div
          className="absolute inset-0 opacity-[0.025]"
          style={{
            backgroundImage:
              "linear-gradient(rgba(196,255,77,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(196,255,77,0.5) 1px, transparent 1px)",
            backgroundSize: "60px 60px",
          }}
        />
      </div>

      <div className="relative z-10 max-w-5xl w-full text-center">
        {/* Eyebrow */}
        <motion.div
          variants={fadeUp(0)}
          initial="hidden"
          animate={animate ?? "visible"}
          whileInView={prefersReducedMotion ? undefined : "visible"}
          viewport={{ once: true }}
          className="inline-flex items-center gap-2 mb-6 px-4 py-2 rounded-full bg-[#1B1E1B] border border-[rgba(198,255,77,0.2)] text-[#C6FF4D] text-sm font-medium"
        >
          <span
            className="w-2 h-2 rounded-full bg-[#C6FF4D] animate-pulse"
            aria-hidden="true"
          />
          Available for opportunities
        </motion.div>

        {/* Name */}
        <motion.h1
          variants={fadeUp(0.1)}
          initial="hidden"
          animate={animate ?? "visible"}
          whileInView={prefersReducedMotion ? undefined : "visible"}
          viewport={{ once: true }}
          className="text-5xl sm:text-6xl md:text-7xl lg:text-8xl font-black tracking-tight text-[#F4F4F0] mb-4 leading-[1.05]"
        >
          {siteConfig.name}
        </motion.h1>

        {/* Role */}
        <motion.p
          variants={fadeUp(0.2)}
          initial="hidden"
          animate={animate ?? "visible"}
          whileInView={prefersReducedMotion ? undefined : "visible"}
          viewport={{ once: true }}
          className="text-2xl sm:text-3xl md:text-4xl font-bold mb-6"
        >
          <span className="gradient-text">{siteConfig.role}</span>
        </motion.p>

        {/* Intro */}
        <motion.p
          variants={fadeUp(0.3)}
          initial="hidden"
          animate={animate ?? "visible"}
          whileInView={prefersReducedMotion ? undefined : "visible"}
          viewport={{ once: true }}
          className="max-w-2xl mx-auto text-[#A7ADA4] text-lg sm:text-xl leading-relaxed mb-10"
        >
          {siteConfig.intro}
        </motion.p>

        {/* CTA Buttons */}
        <motion.div
          variants={fadeUp(0.4)}
          initial="hidden"
          animate={animate ?? "visible"}
          whileInView={prefersReducedMotion ? undefined : "visible"}
          viewport={{ once: true }}
          className="flex flex-col sm:flex-row items-center justify-center gap-4"
        >
          <button
            onClick={() => handleScroll("#projects")}
            className="w-full sm:w-auto px-8 py-4 rounded-xl bg-[#C6FF4D] text-[#101110] font-bold text-base hover:bg-[#d4ff70] active:scale-95 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] focus-visible:ring-offset-2 focus-visible:ring-offset-[#101110] shadow-[0_0_30px_rgba(198,255,77,0.25)]"
          >
            View Projects
          </button>
          <button
            onClick={() => handleScroll("#contact")}
            className="w-full sm:w-auto px-8 py-4 rounded-xl border border-[rgba(166,173,164,0.25)] text-[#F4F4F0] font-bold text-base hover:border-[#C6FF4D] hover:text-[#C6FF4D] active:scale-95 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] focus-visible:ring-offset-2 focus-visible:ring-offset-[#101110]"
          >
            Contact Me
          </button>
          {siteConfig.resumeUrl && (
            <a
              href={siteConfig.resumeUrl}
              download
              className="w-full sm:w-auto px-8 py-4 rounded-xl border border-[rgba(166,173,164,0.25)] text-[#A7ADA4] font-bold text-base hover:border-[#A7ADA4] hover:text-[#F4F4F0] active:scale-95 transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] focus-visible:ring-offset-2 focus-visible:ring-offset-[#101110]"
            >
              Download CV
            </a>
          )}
        </motion.div>
      </div>

      {/* Scroll indicator */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.2, duration: 0.6 }}
        className="absolute bottom-8 left-1/2 -translate-x-1/2"
        aria-hidden="true"
      >
        <button
          onClick={() => handleScroll("#about")}
          className="flex flex-col items-center gap-2 text-[#A7ADA4] hover:text-[#C6FF4D] transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#C6FF4D] rounded p-2"
          aria-label="Scroll to About section"
        >
          <span className="text-xs font-medium tracking-widest uppercase">
            Scroll
          </span>
          <ArrowDown
            size={18}
            className={prefersReducedMotion ? "" : "animate-bounce"}
          />
        </button>
      </motion.div>
    </section>
  );
}
