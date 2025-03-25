package object mason:
    extension [T](value: T)
        def |>[U](f: T => U): U = f(value)
    end extension
