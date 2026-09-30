package com.expense;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** All database queries (JDBC + PreparedStatements). */
public class Dao {

    private static List<Map<String, Object>> rows(PreparedStatement ps) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> m = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) m.put(md.getColumnLabel(i), rs.getObject(i));
                out.add(m);
            }
        }
        return out;
    }

    // ---------- Transactions ----------
    public static List<Map<String, Object>> transactions(String month, String type) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id,type,amount,category,description,tx_date FROM transactions WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (month != null && !month.isEmpty()) { sql.append(" AND DATE_FORMAT(tx_date,'%Y-%m')=?"); args.add(month); }
        if (type != null && !type.isEmpty()) { sql.append(" AND type=?"); args.add(type); }
        sql.append(" ORDER BY tx_date DESC, id DESC");
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) ps.setObject(i + 1, args.get(i));
            return rows(ps);
        }
    }

    public static void addTransaction(String type, BigDecimal amount, String category, String desc, LocalDate date) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO transactions(type,amount,category,description,tx_date) VALUES(?,?,?,?,?)")) {
            ps.setString(1, type); ps.setBigDecimal(2, amount); ps.setString(3, category);
            ps.setString(4, desc); ps.setDate(5, java.sql.Date.valueOf(date));
            ps.executeUpdate();
        }
    }

    public static void updateTransaction(int id, String type, BigDecimal amount, String category, String desc, LocalDate date) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("UPDATE transactions SET type=?,amount=?,category=?,description=?,tx_date=? WHERE id=?")) {
            ps.setString(1, type); ps.setBigDecimal(2, amount); ps.setString(3, category);
            ps.setString(4, desc); ps.setDate(5, java.sql.Date.valueOf(date)); ps.setInt(6, id);
            ps.executeUpdate();
        }
    }

    public static void deleteTransaction(int id) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---------- Budgets ----------
    public static List<Map<String, Object>> budgets(String month) throws SQLException {
        String sql = "SELECT b.id, b.category, b.budget_month AS month, b.limit_amount AS budget, "
            + "COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.type='EXPENSE' AND t.category=b.category "
            + "AND DATE_FORMAT(t.tx_date,'%Y-%m')=b.budget_month),0) AS spent "
            + "FROM budgets b WHERE b.budget_month=? ORDER BY b.category";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, month);
            return rows(ps);
        }
    }

    public static void saveBudget(String category, String month, BigDecimal amount) throws SQLException {
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO budgets(category,budget_month,limit_amount) VALUES(?,?,?) "
                 + "ON DUPLICATE KEY UPDATE limit_amount=VALUES(limit_amount)")) {
            ps.setString(1, category); ps.setString(2, month); ps.setBigDecimal(3, amount);
            ps.executeUpdate();
        }
    }

    public static void deleteBudget(int id) throws SQLException {
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM budgets WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ---------- Summary ----------
    public static Map<String, Object> summary(String month) throws SQLException {
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
        List<Map<String, Object>> cats;
        try (Connection c = Db.get()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT type, SUM(amount) AS total FROM transactions WHERE DATE_FORMAT(tx_date,'%Y-%m')=? GROUP BY type")) {
                ps.setString(1, month);
                for (Map<String, Object> r : rows(ps)) {
                    if ("INCOME".equals(r.get("type"))) income = (BigDecimal) r.get("total");
                    else expense = (BigDecimal) r.get("total");
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT category, SUM(amount) AS total FROM transactions WHERE type='EXPENSE' "
                    + "AND DATE_FORMAT(tx_date,'%Y-%m')=? GROUP BY category ORDER BY total DESC")) {
                ps.setString(1, month);
                cats = rows(ps);
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("income", income); m.put("expense", expense); m.put("balance", income.subtract(expense));
        m.put("categories", cats);
        return m;
    }
}
