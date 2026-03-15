<div align="center">

# 🏪 ASAAS POS - تطبيق نقاط البيع

### تطبيق نقاط البيع لأجهزة الأندرويد من نظام اساس

[![Build & Release](https://github.com/asaassystem/asaas-pos-android/actions/workflows/build-release.yml/badge.svg)](https://github.com/asaassystem/asaas-pos-android/actions/workflows/build-release.yml)
[![Latest Release](https://img.shields.io/github/v/release/asaassystem/asaas-pos-android?label=آخر%20إصدار&color=orange)](https://github.com/asaassystem/asaas-pos-android/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/asaassystem/asaas-pos-android/total?label=التحميلات&color=blue)](https://github.com/asaassystem/asaas-pos-android/releases)

</div>

---

## 📥 تحميل التطبيق

> **الرابط المباشر** - يتحدث تلقائياً مع كل إصدار جديد:

### ⬇️ [تحميل آخر إصدار - APK](https://github.com/asaassystem/asaas-pos-android/releases/latest/download/asaas-pos-latest.apk)

أو انتقل إلى: **[جميع الإصدارات](https://github.com/asaassystem/asaas-pos-android/releases)**

---

## 🌟 المزايا

| الميزة | الوصف |
|--------|-------|
| 🔐 **تسجيل الدخول** | دخول بيوزر وباسوورد الموظف أو المدير |
| 🖥️ **واجهة نقاط البيع** | جميع مزايا لوحة ASAAS الرئيسية |
| 🖨️ **طباعة فورية** | دعم طابعات Bluetooth ESC/POS + Epson |
| 📱 **تحسين الأندرويد** | واجهة محسّنة للمس والشاشات الكبيرة |
| 🔄 **تحديث تلقائي** | الرابط يتحدث مع كل إصدار جديد |
| 🌐 **RTL** | واجهة عربية كاملة من اليمين لليسار |
| 🔒 **آمن** | تشفير بيانات الجلسة والكوكيز |

---

## 📋 متطلبات التشغيل

- Android 7.0 (API 24) أو أحدث
- اتصال بالإنترنت (WiFi أو بيانات)
- حساب في نظام ASAAS

---

## 📲 تعليمات التثبيت

1. **حمّل ملف APK** من الرابط أعلاه
2. افتح **الإعدادات** على جهازك
3. فعّل **"تثبيت التطبيقات من مصادر غير معروفة"**
4. افتح ملف APK واضغط **"تثبيت"**
5. افتح التطبيق وادخل بيانات حسابك

---

## 🔐 بيانات الدخول

| الحقل | القيمة |
|-------|--------|
| رقم المنشأة | رقم المستأجر في ASAAS |
| اسم المستخدم | يوزر الموظف أو المدير |
| كلمة المرور | باسوورد الحساب |

---

## 🛠️ للمطورين - بناء التطبيق

```bash
# استنساخ المشروع
git clone https://github.com/asaassystem/asaas-pos-android.git
cd asaas-pos-android

# البناء
./gradlew assembleDebug

# ملف APK في:
# app/build/outputs/apk/debug/app-debug.apk
```

### متطلبات البناء:
- Android Studio Hedgehog أو أحدث
- JDK 17
- Android SDK 34

---

## 🔄 الإصدارات التلقائية

عند كل push على الفرع الرئيسي، يتم بناء APK جديد تلقائياً.
عند إنشاء تاغ `v*`, يتم نشر إصدار رسمي مع ملف APK.

```bash
# لنشر إصدار جديد
git tag v1.0.1
git push origin v1.0.1
```

---

## 📡 API الاتصال بالخادم

التطبيق يتصل بـ: `https://super.asaas-system.com`

- **تسجيل الدخول**: `/api/auth/login.php`
- **نقاط البيع**: `/pos_sales.php`
- **WebView**: جميع صفحات لوحة التحكم

---

## 🖨️ دعم الطباعة

| النوع | الدعم |
|-------|-------|
| Bluetooth ESC/POS | ✅ |
| Epson TM series | ✅ عبر WiFi/USB |
| Star printers | ✅ |
| طباعة الخادم | ✅ عبر الويب |

---

<div align="center">

## 🏢 نظام اساس

**النظام السحابي لإدارة المنشآت**

[الموقع الرسمي](https://asaas-system.com) • [لوحة التحكم](https://super.asaas-system.com)

MIT License © ASAAS SYSTEM

</div>
