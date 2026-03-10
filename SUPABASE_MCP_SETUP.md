# Supabase MCP Setup Guide 🔌

## What is MCP?

**MCP (Model Context Protocol)** allows Kiro AI to directly connect to your Supabase database. This means I can:
- ✅ Query your database tables
- ✅ See table structures and schemas
- ✅ Help you write SQL queries
- ✅ Debug database issues
- ✅ Suggest optimizations
- ✅ Create and modify tables

---

## Setup Instructions

### Step 1: Get Your Supabase Database Password

1. Go to your Supabase Dashboard: https://supabase.com/dashboard
2. Select your project: **apesvvqntqldihnzmitn**
3. Click on **Settings** (gear icon) in the left sidebar
4. Click on **Database**
5. Scroll down to **Connection string**
6. Look for **Connection pooling** section
7. Copy your database password (you set this when creating the project)

### Step 2: Update the MCP Configuration

1. Open the file: `CommUnity/.kiro/settings/mcp.json`
2. Find this line:
   ```
   "postgresql://postgres.apesvvqntqldihnzmitn:[YOUR-DB-PASSWORD]@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres"
   ```
3. Replace `[YOUR-DB-PASSWORD]` with your actual database password
4. Save the file

**Example:**
If your password is `MySecurePass123`, the line should look like:
```
"postgresql://postgres.apesvvqntqldihnzmitn:MySecurePass123@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres"
```

### Step 3: Restart Kiro (VS Code)

1. Close VS Code completely
2. Reopen VS Code
3. Open your CommUnity project

The MCP server will automatically connect!

---

## Alternative: Get Connection String from Supabase

If you can't find your password, you can get the full connection string:

1. Go to Supabase Dashboard
2. Settings → Database
3. Scroll to **Connection string**
4. Select **Connection pooling** tab
5. Choose **Transaction** mode
6. Copy the connection string (it includes the password)
7. Paste it in the mcp.json file

---

## What I Can Do Once Connected

### Query Your Database
```sql
-- I can run queries like:
SELECT * FROM users WHERE role = 'official';
SELECT COUNT(*) FROM reports WHERE status = 'pending';
```

### See Table Structures
```sql
-- I can describe tables:
DESCRIBE users;
DESCRIBE reports;
DESCRIBE requests;
```

### Help You Debug
- Find missing data
- Check relationships
- Optimize queries
- Fix errors

### Create Tables
```sql
-- I can help create new tables:
CREATE TABLE announcements (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  title TEXT NOT NULL,
  content TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);
```

---

## Your Supabase Info

**Project URL**: https://apesvvqntqldihnzmitn.supabase.co
**Project ID**: apesvvqntqldihnzmitn
**Region**: Southeast Asia (Singapore)

**Connection Details:**
- Host: `aws-0-ap-southeast-1.pooler.supabase.com`
- Port: `6543` (connection pooling)
- Database: `postgres`
- User: `postgres.apesvvqntqldihnzmitn`

---

## Current Tables in Your Database

Based on your app, you should have these tables:

1. **users**
   - id, auth_id, first_name, last_name, email, role, created_at

2. **reports**
   - id, title, description, status, created_at, user_id

3. **requests**
   - id, document_type, purpose, status, created_at, user_id

4. **rewards** (optional)
   - id, user_id, points, created_at

5. **deleted_accounts** (optional)
   - id, email, reason, deleted_at

---

## Testing the Connection

Once you've set up MCP, you can test it by asking me:

- "Show me all users in the database"
- "How many reports are there?"
- "What tables exist in my database?"
- "Describe the users table structure"

I'll be able to query your database directly and give you real-time information!

---

## Security Notes

⚠️ **Important:**
- The database password is sensitive - don't share it
- The `mcp.json` file is in `.kiro/settings/` which should be in `.gitignore`
- Never commit database passwords to GitHub
- Use connection pooling (port 6543) for better performance

✅ **Safe:**
- The anon key in `supabaseClient.js` is safe to commit (it's public)
- MCP connection is local to your VS Code only
- I can only access what your database permissions allow

---

## Troubleshooting

### Issue: "Cannot connect to database"
**Solution**: 
- Check your password is correct
- Make sure you're using the connection pooling URL (port 6543)
- Verify your Supabase project is active

### Issue: "npx not found"
**Solution**:
- Install Node.js globally (not just the local version)
- Or use the full path to npx in the mcp.json

### Issue: "Permission denied"
**Solution**:
- Check your database user has the right permissions
- Make sure you're using the postgres user

---

## Benefits of MCP Connection

### Before MCP:
- ❌ I can only see your code
- ❌ You have to manually check database
- ❌ I can't verify data exists
- ❌ Debugging is slower

### After MCP:
- ✅ I can see your actual data
- ✅ I can query tables directly
- ✅ I can verify issues instantly
- ✅ Faster debugging and development
- ✅ Better suggestions based on real data

---

## Example Usage

Once connected, you can ask me things like:

**"Show me all officials in the database"**
```sql
SELECT first_name, last_name, email 
FROM users 
WHERE role = 'official';
```

**"How many pending reports are there?"**
```sql
SELECT COUNT(*) 
FROM reports 
WHERE status = 'pending';
```

**"What's the structure of the requests table?"**
```sql
\d requests
```

**"Find users who signed up today"**
```sql
SELECT * 
FROM users 
WHERE DATE(created_at) = CURRENT_DATE;
```

---

## Next Steps

1. ✅ Get your Supabase database password
2. ✅ Update `CommUnity/.kiro/settings/mcp.json`
3. ✅ Restart VS Code
4. ✅ Ask me to query your database!

Once set up, I'll be able to help you much more effectively with database-related tasks! 🚀

---

## Need Help?

If you need help finding your password or setting this up, just ask me:
- "How do I find my Supabase password?"
- "Can you help me test the MCP connection?"
- "Show me what tables I have"

I'm here to help! 😊
