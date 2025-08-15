import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.net.URI;
import java.net.http.*;

public class Sprout {
    public static void main(String[] args) {
        if (args.length == 0 || hasFlag(args, "--help") || hasFlag(args, "-h")) {
            System.out.println("🌱 Sprout is ready to plant your next project!"); 
            printHelp();
            return;
        }


        String cmd = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        try {
            switch (cmd) {
                case "new" -> handleNew(rest);
                case "web" -> handleWeb(rest);
                case "gitprep" -> handleGitPrep(rest);
                default -> {
                    System.out.println("Unknown command: " + cmd);
                    printHelp();
                }
            }
        } catch (Exception e) {
            System.err.println("Uh-oh! " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // -------- new (project seeder) --------
    private static void handleNew(String[] args) throws Exception {
        Map<String, String> flags = parseFlags(args);
        String name = flags.getOrDefault("--name", flags.getOrDefault("-n", positional(args)));
        if (name == null || name.isBlank()) name = prompt("Project name");
        String template = flags.getOrDefault("--template", flags.getOrDefault("-t", "")).trim();
        if (template.isEmpty()) template = prompt("Template (java-assignment | web-basic | swift-assignment)");

        Path root = Paths.get(name);
            System.out.println("📁 Creating folder: " + name);
            System.out.println("🌸 Applying template: " + template);
            Files.createDirectories(root);

        switch (template) {
            case "java-assignment" -> scaffoldJavaAssignment(root, name);
            case "web-basic" -> scaffoldWebBasic(root, name, /*tailwind*/ false, /*bootstrap*/ false);
            case "swift-assignment" -> scaffoldSwiftAssignment(root, name);
            default -> {
                System.out.println("🌧️ Oh no! That template doesn’t exist.");
                System.out.println("🌱 Try: --template java-assignment | web-basic | swift-assignment");
                return; // stop gracefully
        }

        }

        if (hasFlag(args, "--git")) {
            gitInit(root);
            System.out.println("✔️ Git initialized and first commit made.");
        }

        if (flags.containsKey("--github")) {
            String user = flags.get("--github");
            String repo = flags.getOrDefault("--repo", name);
            String token = flags.getOrDefault("--token", System.getenv("GITHUB_TOKEN"));
            createGithubRepo(user, repo, token);
            gitAddRemoteAndPush(root, user, repo);
        }

        System.out.println("\n✨ Done! Created project at: " + root.toAbsolutePath());
        System.out.println("🌿 Happy coding!");
        run(root, "code", "-r", ".");
    }

        private static boolean remoteExists(Path path) {
        try {
        ProcessBuilder check = new ProcessBuilder("git", "remote", "get-url", "origin");
        check.directory(path.toFile());
        Process process = check.start();
        return process.waitFor() == 0;
        } catch (Exception e) {
        return false;
         }
    }

    // -------- web (HTML starter shortcut) --------
    private static void handleWeb(String[] args) throws Exception {
        Map<String, String> flags = parseFlags(args);
        String name = flags.getOrDefault("--name", flags.getOrDefault("-n", positional(args)));
        if (name == null || name.isBlank()) name = prompt("Site folder name");
        boolean tailwind = hasFlag(args, "--tailwind");
        boolean bootstrap = hasFlag(args, "--bootstrap");

        Path root = Paths.get(name);
        String preset = bootstrap ? "web-basic (bootstrap)" : (tailwind ? "web-basic (tailwind)" : "web-basic");
            System.out.println("📁 Creating folder: " + name);
            System.out.println("🌸 Applying template: " + preset);
        Files.createDirectories(root);
        scaffoldWebBasic(root, name, tailwind, bootstrap);

        if (hasFlag(args, "--git")) {
            gitInit(root);
            System.out.println("✔️ Git initialized and first commit made.");
        }

        System.out.println("\n✨ Web project ready at: " + root.toAbsolutePath());
        System.out.println("🌿 Happy coding!");
    }

    // -------- gitprep (add git locally + optional GitHub remote) --------
    private static void handleGitPrep(String[] args) throws Exception {
        Map<String, String> flags = parseFlags(args);
        Path root = Paths.get(flags.getOrDefault("--path", "."));
        if (!Files.exists(root)) throw new IllegalArgumentException("Path does not exist: " + root.toAbsolutePath());

        if (!hasFlag(args, "--no-files")) {
            writeIfMissing(root.resolve("README.md"), "# " + root.getFileName() + "\n\nInitialized by sprout gitprep on " + LocalDate.now() + "\n");
            writeIfMissing(root.resolve(".gitignore"), defaultGitignore());
        }

        gitInit(root);
        System.out.println("✔️ Git initialized and first commit made.");

        if (flags.containsKey("--github")) {
        String user = flags.get("--github");
         String repo = flags.getOrDefault("--repo", root.getFileName().toString());
        String token = flags.getOrDefault("--token", System.getenv("GITHUB_TOKEN"));

        createGithubRepo(user, repo, token);
        System.out.println("🚀 GitHub repo created under " + user + "/" + repo);

        gitAddRemoteAndPush(root, user, repo);

        if (remoteExists(root)) {
        run(root, "git", "push", "-u", "origin", "main");
        System.out.println("🔗 Remote linked successfully.");
        } else {
        System.out.println("⚠️  Skipping push: no remote named 'origin' was found.");
        }
        }       
        System.out.println("\n✨ Git is ready in: " + root.toAbsolutePath());
        System.out.println("🌿 Happy coding!");
    }

    // --------- Templates ---------
    private static void scaffoldJavaAssignment(Path root, String name) throws IOException {
        Path src = root.resolve("src");
        Path notes = root.resolve("notes");
        Files.createDirectories(src);
        Files.createDirectories(notes);

        writeIfMissing(root.resolve("README.md"),
                "# " + name + "\n\nCreated on " + LocalDate.now() + " with sprout.\n\n## How to run\n```bash\njavac src/Main.java && java -cp src Main\n```\n");
        writeIfMissing(root.resolve(".gitignore"), defaultGitignore());
        writeIfMissing(notes.resolve("requirements.md"), "## Requirements\n\n- [ ] Goals\n- [ ] Input/Output\n- [ ] Edge cases\n");

        writeIfMissing(src.resolve("Main.java"),
                """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Hello, %s! Let's code.\\n");
                    }
                }
                """.formatted(name));
    }

    private static void scaffoldSwiftAssignment(Path root, String name) throws IOException {
        Path src = root.resolve("Sources/App");
        Files.createDirectories(src);
        writeIfMissing(root.resolve("README.md"),
                "# " + name + " (Swift Assignment)\n\nCreated on " + LocalDate.now() + " with sprout.\n");
        writeIfMissing(root.resolve(".gitignore"), defaultGitignoreSwift());
        writeIfMissing(src.resolve("main.swift"),
                """
                import Foundation

                print("Hello, %(name)s! Time to Swift.")
                """.replace("%(name)s", name));
    }

    private static void scaffoldWebBasic(Path root, String name, boolean tailwind, boolean bootstrap) throws IOException {
        Path assets = root.resolve("assets");
        Files.createDirectories(assets);

        String cssLink = "<link rel=\"stylesheet\" href=\"style.css\">";
        String jsLink = "<script src=\"app.js\" defer></script>";
        String frameworkHead = "";
        if (tailwind) {
            frameworkHead = """
                <!-- Tailwind via CDN for quick demos -->
                <script src="https://cdn.tailwindcss.com"></script>
                """;
        } else if (bootstrap) {
            frameworkHead = """
                <!-- Bootstrap via CDN -->
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" defer></script>
                """;
        }

        writeIfMissing(root.resolve("index.html"),
                """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%s</title>
                  %s
                  %s
                </head>
                <body>
                  <main class="%s">
                    <h1>%s</h1>
                    <p>Bootstrapped by sprout web on %s.</p>
                  </main>
                  %s
                </body>
                </html>
                """.formatted(name,
                        frameworkHead,
                        cssLink,
                        tailwind ? "p-6 max-w-xl mx-auto" : (bootstrap ? "container py-4" : ""),
                        name,
                        LocalDate.now().toString(),
                        jsLink));

        writeIfMissing(root.resolve("style.css"),
                """
                :root {
                  --ink: #1f2937;
                  --bg: #f8fafc;
                  --accent: #22c55e;
                }
                html, body { margin: 0; padding: 0; background: var(--bg); color: var(--ink); font-family: system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; }
                main { padding: 2rem; }
                h1 { margin-bottom: 0.5rem; }
                """);

        writeIfMissing(root.resolve("app.js"),
                """
                console.log("sprout web scaffold ready");
                """);

        writeIfMissing(root.resolve("README.md"),
                """
                # %s (Web)
                Generated by sprout on %s.

                ## Files
                - index.html
                - style.css
                - app.js
                - assets/

                ## Quick Preview
                Use a simple server, e.g. Python 3:
                ```bash
                python3 -m http.server
                ```
                """.formatted(name, LocalDate.now().toString()));

        writeIfMissing(root.resolve(".gitignore"), defaultGitignoreWeb());
    }

    // -------- Git helpers --------
    private static void gitInit(Path root) throws IOException, InterruptedException {
        if (!Files.exists(root.resolve(".git"))) {
            run(root, "git", "init");
        }
        run(root, "git", "add", ".");
        run(root, "git", "commit", "-m", "chore: initialize project with sprout");
    }

    private static void gitAddRemoteAndPush(Path root, String user, String repo) throws IOException, InterruptedException {
        String remote = "https://github.com/" + user + "/" + repo + ".git";

        if (remoteExists(root)) {
        run(root, "git", "remote", "remove", "origin");
        } else {
        System.out.println("🔍 No existing 'origin' remote found — skipping removal.");
        }

        run(root, "git", "remote", "add", "origin", remote);
        run(root, "git", "branch", "-M", "main");
        run(root, "git", "push", "-u", "origin", "main");
    }

    // -------- GitHub API (optional) --------
    private static void createGithubRepo(String user, String repo, String token) throws Exception {
        if (token == null || token.isBlank()) {
            System.out.println("⚠️  No GitHub token found. Set env var GITHUB_TOKEN or pass --token <VALUE>.");
            System.out.println("   Skipping remote repo creation—local git is still set up.");
            return;
        }
        String payload = "{\"name\":\"" + repo + "\",\"private\":false,\"auto_init\":false}";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user/repos"))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            System.out.println("✅ Created GitHub repo: " + user + "/" + repo);
        } else if (response.statusCode() == 422) {
            System.out.println("ℹ️  Repo likely already exists on GitHub; proceeding to add remote.");
        } else {
            System.out.println("⚠️  GitHub API response (" + response.statusCode() + "): " + response.body());
            System.out.println("   You can still add the remote manually later.");
        }
    }

    // -------- Utilities --------
    private static String positional(String[] args) {
        for (String a : args) if (!a.startsWith("-")) return a;
        return null;
    }

    private static Map<String, String> parseFlags(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    m.put(a, args[++i]);
                } else {
                    m.put(a, "true");
                }
            } else if (a.startsWith("-")) {
                if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    m.put(a, args[++i]);
                } else {
                    m.put(a, "true");
                }
            }
        }
        return m;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) if (a.equals(flag)) return true;
        return false;
    }

    private static String prompt(String label) throws IOException {
        System.out.print(label + ": ");
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        return r.readLine();
    }

    private static void writeIfMissing(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent() == null ? Paths.get(".") : path.getParent());
            Files.write(path, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  + " + path);
        } else {
            System.out.println("  = (exists) " + path);
        }
    }

    private static int run(Path dir, String... cmd) throws IOException, InterruptedException {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(dir.toFile());
            pb.inheritIO();
            Process p = pb.start();
            return p.waitFor();
        } catch (IOException e) {
            // best-effort for remove remote etc.
            if (cmd.length >= 3 && "remote".equals(cmd[1]) && "remove".equals(cmd[2])) return 0;
            throw e;
        }
    }

    private static void printHelp() {
    System.out.println("""
    ╭──────────────────────────────────────────────────────────╮
    │ 🌱 Welcome to Sprout – your cozy coding companion! 🌿     │
    ╰──────────────────────────────────────────────────────────╯

    Sprout helps you plant, grow, and share your Java projects
    from the comfort of your terminal window. ✨

    📦 Commands:
    ──────────────────────────────────────────────
    new <name> [--template <template>] [options]
        🌼 Create a new project folder with optional template files.

    web <search terms>
        🌐 Open a browser tab to search or plan your project.

    gitprep <repo-name> [--github <username>]
        🌿 Initialize a git repo and push to GitHub.

    🧷 Flags:
    ──────────────────────────────────────────────
    --template <template>   Use a starter template (e.g., java-assignment)
    -git                    Initialize Git in your new project folder
    -github <username>     Link to GitHub and push repo
    --help or -h           Show this helpful little garden guide

    ✨ Example:
    sprout new CozyQuest --template java-assignment -git -github mintbanshee

    Happy coding! May your ideas bloom! 🌸
    """);
}

    private static String defaultGitignore() {
        return """
            # Java
            *.class
            *.jar
            *.log
            .idea/
            .vscode/
            out/
            target/
            bin/
            """;
    }

    private static String defaultGitignoreSwift() {
        return """
            .build/
            Packages/
            xcuserdata/
            DerivedData/
            .DS_Store
            """;
    }

    private static String defaultGitignoreWeb() {
        return """
            .DS_Store
            node_modules/
            dist/
            .idea/
            .vscode/
            """;
    }
}