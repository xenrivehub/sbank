# sBank - A Fun and Handy Banking Plugin for your Minecraft Servers

**sBank** is a fun and handy banking plugin for Minecraft servers that lets players manage their money, take out loans, and earn interest on their savings! With seamless integration with **Vault** for economy management, sBank brings a whole new level of financial adventure to your gameplay. Dive in and enjoy all the features that make managing your finances a breeze!

---

## **Features**

- **Bank Accounts:** Players can easily create and manage their own bank accounts, putting their finances at their fingertips.
- **Deposits and Withdrawals:** Effortlessly deposit and withdraw money from your bank account whenever you need to!
- **Loans:** Need some extra cash? Players can take out loans and repay them over time with interest.
- **Interest:** Watch your savings grow! Players earn interest on their bank balances at customizable intervals.
- **Death Penalty:** Beware! Players may lose a percentage of their money upon death, with the option to configure this via permissions.
- **GUI Integration:** Enjoy a user-friendly graphical interface for hassle-free management of bank accounts and loans.
- **Configurable Settings:** All features, including loans, interest rates, and the death penalty, can be easily enabled or disabled through the configuration settings. Plus, everything within the plugin is fully customizable via the config file.
- **Database Support:** Enjoy flexibility with support for both **MySQL** and **SQLite**, allowing players to choose their preferred database system for safely storing financial data.

---

## **Commands**

- `/sbank` - Main command of sBank GUI which you can manage all things.
- `/sbank debt` - Check your current debt status.
- `/sbank reload` - Reload the plugin configuration.

---

## **Permissions**

- `sbank.use` - Allows the player to use the sBank GUI.
- `sbank.loan` - Allows the player to take out loans.
- `sbank.interest` - Allows the player to earn interest on their bank balance.
- `sbank.interest.<percentage>` - Allow players or roles to have custom interest rates.
- `sbank.dontlosemoney` - Prevents the player from losing money upon death.
- `sbank.admin` - Allows the player to use the reload command.
- `sbank.bypass.banker` - Allows players to use the `/bank` command when a banker npc is enabled.
---

## **Dependencies**

- **Vault** (Required)
- **Citizens** (Optional)
---

## **Support**

If you need any help, feel free to join and ask on [**Discord**](https://discord.gg/FSTJhYPg9c).