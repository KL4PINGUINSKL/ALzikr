// ALzikr Desk: a small window on the laptop for the site's private pages —
// «التنزيلات» (alzikr.alpharmasy.com/stats) and «الرسائل» (/reports, problems and feature requests from the app).
// The password (the Worker's STATS_KEY) is asked once and kept encrypted for this Windows user (DPAPI).
using System.Security.Cryptography;
using System.Text;
using Microsoft.Web.WebView2.WinForms;

namespace ALzikrDesk;

static class Program
{
    [STAThread]
    static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
    }
}

static class Key
{
    static readonly string Dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "ALzikrDesk");
    static readonly string File_ = Path.Combine(Dir, "key.bin");
    /** The key kept for this Windows user, else the one the site's scripts keep (D:\ALzikr\cloudflare\.stats-key). */
    public static string? Load()
    {
        try { if (File.Exists(File_)) return Encoding.UTF8.GetString(ProtectedData.Unprotect(File.ReadAllBytes(File_), null, DataProtectionScope.CurrentUser)); }
        catch { }
        try
        {
            const string repo = @"D:\ALzikr\cloudflare\.stats-key";
            if (File.Exists(repo)) { var k = File.ReadAllText(repo).Trim(); if (k.Length > 0) { Save(k); return k; } }
        }
        catch { }
        return null;
    }
    public static void Save(string k)
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllBytes(File_, ProtectedData.Protect(Encoding.UTF8.GetBytes(k), null, DataProtectionScope.CurrentUser));
    }
    public static string? Ask(IWin32Window owner, string why)
    {
        using var f = new Form { Text = "كلمة السر", RightToLeft = RightToLeft.Yes, RightToLeftLayout = true, FormBorderStyle = FormBorderStyle.FixedDialog,
            MinimizeBox = false, MaximizeBox = false, StartPosition = FormStartPosition.CenterParent, ClientSize = new Size(380, 150), Font = new Font("Segoe UI", 10) };
        var l = new Label { Text = why, AutoSize = false, Bounds = new Rectangle(14, 12, 352, 44) };
        var t = new TextBox { UseSystemPasswordChar = true, Bounds = new Rectangle(14, 60, 352, 28) };
        var ok = new Button { Text = "حفظ", DialogResult = DialogResult.OK, Bounds = new Rectangle(14, 104, 100, 32) };
        var no = new Button { Text = "إلغاء", DialogResult = DialogResult.Cancel, Bounds = new Rectangle(122, 104, 100, 32) };
        f.Controls.AddRange(new Control[] { l, t, ok, no }); f.AcceptButton = ok; f.CancelButton = no;
        if (f.ShowDialog(owner) != DialogResult.OK || t.Text.Trim().Length == 0) return null;
        Save(t.Text.Trim()); return t.Text.Trim();
    }
}

sealed class MainForm : Form
{
    const string Site = "https://alzikr.alpharmasy.com";
    readonly WebView2 web = new() { Dock = DockStyle.Fill };
    readonly ToolStripButton stats = new("📊 التنزيلات"), reports = new("💬 الرسائل");
    string? key = Key.Load();
    int tries;

    public MainForm()
    {
        Text = "ALzikr — التنزيلات والرسائل";
        RightToLeft = RightToLeft.Yes; RightToLeftLayout = true;
        Font = new Font("Segoe UI", 10);
        Size = new Size(900, 820); StartPosition = FormStartPosition.CenterScreen;
        try { Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); } catch { }
        var bar = new ToolStrip { GripStyle = ToolStripGripStyle.Hidden, Padding = new Padding(6, 4, 6, 4), ImageScalingSize = new Size(20, 20) };
        var refresh = new ToolStripButton("⟳ تحديث");
        var pass = new ToolStripButton("🔑 كلمة السر") { Alignment = ToolStripItemAlignment.Right };
        var site = new ToolStripButton("🌐 صفحة التحميل") { Alignment = ToolStripItemAlignment.Right };
        stats.Click += (_, _) => Go("/stats");
        reports.Click += (_, _) => Go("/reports");
        refresh.Click += (_, _) => web.Reload();
        pass.Click += (_, _) => { key = Key.Ask(this, "كلمة سر صفحتي التنزيلات والرسائل (STATS_KEY):") ?? key; web.Reload(); };
        site.Click += (_, _) => Go("/download.html");
        bar.Items.AddRange(new ToolStripItem[] { stats, reports, new ToolStripSeparator(), refresh, pass, site });
        Controls.Add(web); Controls.Add(bar);
        Shown += async (_, _) => await Start();
    }

    async Task Start()
    {
        var data = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "ALzikrDesk", "WebView2");
        var env = await Microsoft.Web.WebView2.Core.CoreWebView2Environment.CreateAsync(null, data);
        await web.EnsureCoreWebView2Async(env);
        var core = web.CoreWebView2;
        core.Settings.IsStatusBarEnabled = false;
        // The pages ask for a password (HTTP Basic): answer with the saved one; ask again if it was refused.
        core.BasicAuthenticationRequested += (_, e) =>
        {
            if (key == null || tries++ > 0) key = Key.Ask(this, tries > 1 ? "كلمة السر غير صحيحة، أعد كتابتها:" : "كلمة سر صفحتي التنزيلات والرسائل (STATS_KEY):");
            if (key == null) { e.Cancel = true; return; }
            e.Response.UserName = "admin"; e.Response.Password = key;
        };
        core.NavigationCompleted += (_, _) => tries = 0;
        Go("/reports");
    }

    void Go(string path)
    {
        stats.Checked = path == "/stats"; reports.Checked = path == "/reports";
        tries = 0;
        web.CoreWebView2?.Navigate(Site + path);
    }
}
