package automate.profit.autocoin.payment

interface UserPaymentRepository {

}

class FileUserPaymentRepository: UserPaymentRepository {

}

class UserPaymentService(private val userPaymentRepository: UserPaymentRepository) {
}