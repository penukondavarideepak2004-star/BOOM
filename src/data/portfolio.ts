/**
 * ============================================================
 *  PORTFOLIO CONFIGURATION — edit this file to personalize
 *  your entire website. All components read from here.
 * ============================================================
 */

export const siteConfig = {
  // ──────────────────────────────────────────────────────────
  // SEO & Metadata
  // ──────────────────────────────────────────────────────────
  seo: {
    title: "YOUR_NAME — YOUR_ROLE",
    description:
      "Personal portfolio of YOUR_NAME — a YOUR_ROLE passionate about building great digital experiences.",
    url: "https://your-domain.com", // replace after deployment
    twitterHandle: "@your_handle",   // optional
  },

  // ──────────────────────────────────────────────────────────
  // Personal Identity
  // ──────────────────────────────────────────────────────────
  name: "YOUR_NAME",
  initials: "YN",                    // used when no logo image is set
  role: "YOUR_ROLE",                 // e.g. "Full-Stack Engineer"
  tagline: "YOUR_TAGLINE",           // short punchy line under the role

  /** 1–2 sentences shown in the Hero below the role */
  intro:
    "YOUR_INTRO — Replace this with a short, engaging sentence about who you are and what you love building.",

  /** Profile image path (place file in public/images/) — set null to hide */
  profileImage: null as string | null, // e.g. "/images/profile.jpg"

  /** Resume PDF path (place file in public/resume/) — set null to hide button */
  resumeUrl: null as string | null,    // e.g. "/resume/YOUR_NAME_CV.pdf"

  // ──────────────────────────────────────────────────────────
  // About Section
  // ──────────────────────────────────────────────────────────
  about: {
    heading: "About Me",
    paragraphs: [
      "YOUR_BIO_PARAGRAPH_1 — Replace this with a short paragraph about your background, where you studied or worked, and what drives you professionally.",
      "YOUR_BIO_PARAGRAPH_2 — Add a second paragraph about your interests, side projects, or anything that makes you distinctive.",
    ],
    highlights: [
      "📍 YOUR_LOCATION",
      "🎓 YOUR_EDUCATION",
      "💼 YOUR_EXPERIENCE",
      "🌐 YOUR_LANGUAGE_OR_AVAILABILITY",
    ],
  },

  // ──────────────────────────────────────────────────────────
  // Skills Section
  // ──────────────────────────────────────────────────────────
  skills: [
    {
      category: "Languages",
      items: ["TypeScript", "JavaScript", "Python", "YOUR_LANGUAGE"],
    },
    {
      category: "Frontend",
      items: ["React", "Next.js", "Tailwind CSS", "YOUR_FRAMEWORK"],
    },
    {
      category: "Backend",
      items: ["Node.js", "Express", "PostgreSQL", "YOUR_TOOL"],
    },
    {
      category: "Tools & DevOps",
      items: ["Git", "Docker", "Vercel", "YOUR_TOOL"],
    },
    {
      category: "Design",
      items: ["Figma", "YOUR_DESIGN_TOOL"],
    },
  ],

  // ──────────────────────────────────────────────────────────
  // Projects Section
  // ──────────────────────────────────────────────────────────
  projects: [
    {
      id: "project-1",
      title: "YOUR_PROJECT_TITLE",
      description:
        "YOUR_PROJECT_DESCRIPTION — A short paragraph about what this project does, the problem it solves, and any notable outcomes.",
      tech: ["React", "TypeScript", "YOUR_TECH"],
      /** Place image in public/images/ — set null to show gradient placeholder */
      image: null as string | null,
      liveUrl: null as string | null,   // e.g. "https://your-project.vercel.app"
      repoUrl: null as string | null,   // e.g. "https://github.com/you/project"
      featured: true,
    },
    {
      id: "project-2",
      title: "YOUR_PROJECT_TITLE",
      description:
        "YOUR_PROJECT_DESCRIPTION — Describe the second project briefly.",
      tech: ["Node.js", "Express", "YOUR_TECH"],
      image: null as string | null,
      liveUrl: null as string | null,
      repoUrl: null as string | null,
      featured: true,
    },
    {
      id: "project-3",
      title: "YOUR_PROJECT_TITLE",
      description:
        "YOUR_PROJECT_DESCRIPTION — Describe the third project briefly.",
      tech: ["Python", "PostgreSQL", "YOUR_TECH"],
      image: null as string | null,
      liveUrl: null as string | null,
      repoUrl: null as string | null,
      featured: false,
    },
  ],

  // ──────────────────────────────────────────────────────────
  // Contact Section
  // ──────────────────────────────────────────────────────────
  contact: {
    heading: "Get In Touch",
    subheading:
      "Have a project in mind, a question, or just want to say hi? I'd love to hear from you.",
    email: "your@email.com",   // REPLACE — used in mailto: links
  },

  // ──────────────────────────────────────────────────────────
  // Social Links
  // Set url to null to hide that icon from the footer/header.
  // ──────────────────────────────────────────────────────────
  social: [
    {
      name: "GitHub",
      url: "https://github.com/YOUR_GITHUB",   // set null to hide
      icon: "github",
    },
    {
      name: "LinkedIn",
      url: "https://linkedin.com/in/YOUR_LINKEDIN", // set null to hide
      icon: "linkedin",
    },
    {
      name: "Twitter",
      url: null as string | null,               // set a URL or leave null
      icon: "twitter",
    },
  ],
} as const;

export type SocialLink = (typeof siteConfig.social)[number];
export type Project = (typeof siteConfig.projects)[number];
export type SkillGroup = (typeof siteConfig.skills)[number];
