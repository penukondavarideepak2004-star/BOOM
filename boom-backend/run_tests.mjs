import { spawn } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const testFile = path.join(__dirname, "tests", "api_security.test.ts");

console.log("[BOOM Test Runner] Running test suite:", testFile);

const child = spawn(
  process.execPath,
  ["--experimental-strip-types", "--test", testFile],
  { stdio: "inherit" }
);

child.on("exit", (code) => {
  if (code === 0) {
    console.log("\n[BOOM Test Runner] ALL ACCEPTANCE TESTS PASSED! ✅");
    process.exit(0);
  } else {
    console.error(`\n[BOOM Test Runner] Tests failed with exit code ${code} ❌`);
    process.exit(code || 1);
  }
});
