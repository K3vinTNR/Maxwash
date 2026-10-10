package org.umn.maxwash.data

object FormValidation {
    fun name(value: String): String? = when {
        value.isBlank() -> "Nama lengkap wajib diisi"
        value.trim().length < 2 -> "Nama minimal 2 karakter"
        else -> null
    }
    fun phone(value: String): String? = when {
        value.isBlank() -> "Nomor handphone wajib diisi"
        value.any { it !in '0'..'9' } -> "Nomor handphone hanya boleh berisi angka"
        value.length !in 10..13 -> "Nomor handphone harus 10–13 angka"
        !value.startsWith("08") && !value.startsWith("62") -> "Gunakan awalan 08 atau 62"
        else -> null
    }
    fun email(value: String): String? = when {
        value.isBlank() -> "Email wajib diisi"
        value.trim().substringBefore('@').let { it.startsWith('.') || it.endsWith('.') || ".." in it } -> "Format email tidak valid"
        !Regex("^[A-Za-z0-9.!#%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$").matches(value.trim()) -> "Format email tidak valid"
        else -> null
    }
    fun password(value: String): String? = when {
        value.isBlank() -> "Password wajib diisi"
        value.length < 8 -> "Password minimal 8 karakter"
        else -> null
    }
    fun confirmation(value: String, password: String): String? = when {
        value.isBlank() -> "Konfirmasi password wajib diisi"
        value != password -> "Konfirmasi password tidak cocok"
        else -> null
    }
    fun registration(name: String, phone: String, email: String, password: String, confirmation: String) =
        listOf(this.name(name), this.phone(phone), this.email(email), this.password(password), this.confirmation(confirmation, password)).all { it == null }
}
