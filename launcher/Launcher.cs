using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

internal static class Program
{
    [STAThread]
    static int Main()
    {
        string exeDir = AppDomain.CurrentDomain.BaseDirectory;
        string jarPath = Path.Combine(exeDir, "rd-132211.jar");

        if (!File.Exists(jarPath))
        {
            MessageBox.Show(
                "Could not find rd-132211.jar next to this executable.\n\nExpected:\n" + jarPath,
                "rd-132211",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            return 1;
        }

        string java = FindJava();
        if (java == null)
        {
            MessageBox.Show(
                "Java was not found.\n\nInstall Java 8 or newer and ensure it is on PATH.",
                "rd-132211",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            return 1;
        }

        try
        {
            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = java;
            psi.Arguments = "-jar \"" + jarPath + "\"";
            psi.WorkingDirectory = exeDir;
            psi.UseShellExecute = false;

            using (Process process = Process.Start(psi))
            {
                if (process == null)
                {
                    MessageBox.Show("Failed to start Java.", "rd-132211", MessageBoxButtons.OK, MessageBoxIcon.Error);
                    return 1;
                }
                process.WaitForExit();
                return process.ExitCode;
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show("Failed to launch:\n\n" + ex.Message, "rd-132211", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }

    static string FindJava()
    {
        string pathEnv = Environment.GetEnvironmentVariable("PATH") ?? "";
        foreach (string dir in pathEnv.Split(Path.PathSeparator))
        {
            if (string.IsNullOrWhiteSpace(dir))
            {
                continue;
            }
            string candidate = Path.Combine(dir.Trim(), "java.exe");
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrWhiteSpace(javaHome))
        {
            string candidate = Path.Combine(javaHome, "bin", "java.exe");
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        string[] extras = new string[]
        {
            @"C:\Program Files (x86)\Common Files\Oracle\Java\javapath\java.exe",
            @"C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
        };
        foreach (string candidate in extras)
        {
            if (File.Exists(candidate))
            {
                return candidate;
            }
        }

        return null;
    }
}
